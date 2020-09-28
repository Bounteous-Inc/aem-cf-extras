package com.bounteous.core.services.impl;

import com.bounteous.core.common.GraphQLServiceException;
import com.bounteous.core.models.graphql.GraphQLBase;
import com.bounteous.core.models.graphql.GraphQLContentFragment;
import com.bounteous.core.models.graphql.GraphQLQueryRoot;
import com.bounteous.core.services.GraphQLService;
import com.bounteous.core.utils.ContentFragmentUtils;

import com.drew.lang.annotations.NotNull;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaPrinter;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.models.factory.ModelFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static graphql.ExecutionInput.newExecutionInput;

/**
 * This is the main service to execute GraphQL queries against the content fragments and models of the current AEM instance.
 */
@Component(
        service = {GraphQLService.class, ResourceChangeListener.class},
        property = {
                Constants.SERVICE_DESCRIPTION  + "=" + "Service to execute a given GraphQL query against content fragments.",
                ResourceChangeListener.PATHS   + "=" + "glob:/conf/*/settings/dam/cfm/models/**",
                ResourceChangeListener.PATHS   + "=" + "glob:/content/dam/**",
                ResourceChangeListener.CHANGES + "=" + "ADDED",
                ResourceChangeListener.CHANGES + "=" + "REMOVED",
                ResourceChangeListener.CHANGES + "=" + "CHANGED"
        }
)
@Designate(ocd = GraphQLServiceImpl.Config.class)
public class GraphQLServiceImpl implements GraphQLService, ResourceChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(GraphQLServiceImpl.class);

    //TODO: Get this from the OSGI configuration `org.apache.sling.caconfig.resource.impl.def.DefaultConfigurationResourceResolvingStrategy`
    public static final String CONF_PATH = "/conf";

    public static final String GENERIC_QUERY_BY_ID = "{ %Namespace% { %ModelName%ById ( id: \"%Id%\" ) { %Attributes% } } }";

    public static final long DEFAULT_DOCUMENT_CACHE_MAX_SIZE       = 10_000;
    public static final long DEFAULT_DOCUMENT_CACHE_EXPIRE         = 1440;    // Minutes
    public static final long DEFAULT_GRAPHQL_SCHEMA_CACHE_EXPIRE   = 1440;    // Minutes
    public static final long DEFAULT_QUERY_RESULT_CACHE_MAX_SIZE   = 1_000;
    public static final long DEFAULT_QUERY_RESULT_CACHE_EXPIRE     = 1440;    // Minutes

    private static final String GRAPHQL_SCHEMA_KEY = "graphQLSchema";

    PreparsedDocumentProvider preparsedCache = new PreparsedDocumentProvider() {
        @Override
        public PreparsedDocumentEntry getDocument(ExecutionInput executionInput, Function<ExecutionInput, PreparsedDocumentEntry> computeFunction) {
            Function<String, PreparsedDocumentEntry> mapCompute = key -> computeFunction.apply(executionInput);
            return parsedDocumentEntryCache.get(executionInput.getQuery(), mapCompute);
        }
    };

    private Cache<String, PreparsedDocumentEntry> parsedDocumentEntryCache;
    private LoadingCache<String, GraphQLSchema> graphQLSchemaCache;
    private LoadingCache<String, Map<String, Object>> graphQLResultCache;

    @Inject
    @Reference
    private ResourceResolverFactory resolverFactory;

    @Inject
    @Reference
    private ModelFactory modelFactory;

    /**
     * Initialize the service by defining the caching instances.
     * @param config The OSGI config object for this service.
     */
    @Activate
    @Modified
    @SuppressWarnings("unused")
    public void activate(Config config) {
        // Cache of the parsed/validated query
        parsedDocumentEntryCache = Caffeine.newBuilder()
            .maximumSize(config.documentCacheMaxSize())
            .expireAfterWrite(DEFAULT_DOCUMENT_CACHE_EXPIRE, TimeUnit.MINUTES)
            .build();

        // Cache of the GraphQL Schema
        graphQLSchemaCache = Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(config.graphQLSchemaCacheExpire(), TimeUnit.MINUTES)
                .build(key -> buildSchema());

        // Cache of the GraphQL Query Result.  Note that this is a very basic cache that uses a base64 encoding of the query as a key.  Therefore
        // every query that is cached must be the exact same String.
        // TODO: Make this cache more intelligent to match queries that return the same results yet are not necessarily the exact same string.
        graphQLResultCache = Caffeine.newBuilder()
            .maximumSize(config.queryResultCacheMaxSize())
            .expireAfterWrite(config.queryResultCacheExpire(), TimeUnit.MINUTES)
            .build(this::executeQuery);
    }

    /**
     * Invalidate the schema and parsed query caches when a Content Fragment Model is added/updated/deleted.
     * Invalidate the query results cache when a content fragment is udpated.
     * @param list A list of resource change events.
     */
    @Override
    public void onChange(List<ResourceChange> list) {
        list.forEach((change) -> {
            final String changePath = change.getPath();
            if (changePath.startsWith(CONF_PATH)) {
                graphQLSchemaCache.invalidateAll();
                parsedDocumentEntryCache.invalidateAll();
                graphQLResultCache.invalidateAll();
            } else {
                try (ResourceResolver resolver = getCfManagementResourceResolver()) {
                    if (ContentFragmentUtils.isContentFragment(changePath, resolver)) {
                        graphQLResultCache.invalidateAll();
                    }
                }
            }
        });
    }

    /**
     * Execute a GraphQL query with a CF service user resource resolver.
     * @param queryString The query to execute.
     * @return A Map representing the results of the query.
     */
    @Override
    public Map<String, Object> executeQuery(@NotNull final String queryString) {
        try (ResourceResolver resolver = getCfManagementResourceResolver()) {
            return executeQuery(queryString, resolver);
        }
    }

    /**
     * Execute a GraphQL query with the given resource resolver.
     * @param queryString The query to execute.
     * @param resolver The resolver to use for the query.
     * @return A Map representing the results of the query.
     */
    private Map<String, Object> executeQuery(@NotNull final String queryString, @NotNull final ResourceResolver resolver) {
        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchemaCache.get(GRAPHQL_SCHEMA_KEY))
                .preparsedDocumentProvider(preparsedCache)
                .build();

        GraphQLQueryRoot graphQLQueryRoot = getGraphQLQueryRoot(resolver);
        GraphQLBase.DataFetcherContext context = new GraphQLBase.DataFetcherContext(resolver);
        ExecutionInput executionInput = newExecutionInput(queryString)
                .context(context)
                .root(graphQLQueryRoot)
                .build();
        ExecutionResult executionResult = graphQL.execute(executionInput);

        return executionResult.toSpecification();
    }

    /**
     * A convenience method that constructs a GraphQL Query to pull the content fragment at the given path including the list of attributes provided.
     * @param cfPath The path of the content fragment.
     * @param attributeList The attributes to return.
     * @return A map of attributes to values.
     */
    @Override
    public Map<String, Object> executeQueryForCfPath(@NotNull final String cfPath,
                                                     @NotNull final List<String> attributeList) {
        try (ResourceResolver resolver = getCfManagementResourceResolver()) {
            GraphQLContentFragment.ContentFragmentInfo cfInfo = getGraphQLContentFragmentInfo(cfPath, resolver);

            if (cfInfo == null) {
                return new HashMap<>();
            }

            String namespace = cfInfo.getContentFragmentModelInfo().getConfigNamespaceInfo().getGraphQLName();
            String attributes = buildAttributesFromList(attributeList);
            String query = GENERIC_QUERY_BY_ID
                    .replaceAll("%Namespace%", namespace)
                    .replaceAll("%Id%", cfInfo.getId())
                    .replaceAll("%ModelName%", cfInfo.getContentFragmentModelInfo().getGraphQLName())
                    .replaceAll("%Attributes%", attributes);

            return executeQuery(query);
        }
    }

    /**
     * Execute the give GraphQL Query, returning the results in a JSON string (as apposed to a Map)
     * If the passed in query string is blank then return the schema.
     * @param queryString The GraphQL query or blank for a schema.
     * @return The GraphQL results or a schema in JSON string format.
     */
    @Override
    public String executeQueryAsJson(@NotNull final String queryString) {
        try (ResourceResolver resolver = getCfManagementResourceResolver()) {
            if (StringUtils.isBlank(queryString)) {
                GraphQLSchema graphQLSchema = graphQLSchemaCache.get(GRAPHQL_SCHEMA_KEY, k -> buildSchema(resolver));
                return new SchemaPrinter().print(graphQLSchema);
            }

            //TODO: Make pretty print configurable
            return new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(graphQLResultCache.get(queryString));
        } catch (Exception e) {
            LOG.error(Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }

    /**
     * Get a ContentFragmentInfo object that is not tied to a resource resolver.
     * @param cfPath The path of the content fragment.
     * @return A pojo representing a content fragment.
     */
    @Override
    public GraphQLContentFragment.ContentFragmentInfo getGraphQLContentFragmentInfo(@NotNull final String cfPath) {
        try (ResourceResolver resolver = getCfManagementResourceResolver()) {
            return getGraphQLContentFragmentInfo(cfPath, resolver);
        }
    }

    /**
     * Get a ContentFragmentInfo object that is not tied to a resource resolver.  Use the given resolver to access the repository.
     * @param cfPath The path of the content fragment.
     * @param resolver The resolver to use to fetch content.
     * @return A pojo representing a content fragment.
     */
    private GraphQLContentFragment.ContentFragmentInfo getGraphQLContentFragmentInfo(@NotNull final String cfPath, ResourceResolver resolver) {
        Resource cfResource = resolver.getResource(cfPath);
        if (cfResource != null) {
            GraphQLContentFragment graphQLContentFragment = ContentFragmentUtils.createModel(modelFactory, cfResource, GraphQLContentFragment.class);
            if (graphQLContentFragment != null && graphQLContentFragment.isValid()) {
                return graphQLContentFragment.getContentFragmentInfo();
            }
        }

        return null;
    }

    /**
     * Wrap call to buildSchema() with a closeable ResourceResolver.
     * @return The GraphQL schema for this AEM instance.
     */
    private GraphQLSchema buildSchema() {
        try (ResourceResolver resolver = getCfManagementResourceResolver()) {
            return buildSchema(resolver);
        }
    }

    /**
     * Build the GraphQL schema which tells the client what is available for querying.
     * @param resolver The resolver to use to fetch content.
     * @return The GraphQL schema for this AEM instance.
     */
    private GraphQLSchema buildSchema(ResourceResolver resolver) {
        try {
            RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();
            TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();

            GraphQLQueryRoot graphQLQueryRoot = getGraphQLQueryRoot(resolver);
            graphQLQueryRoot.addFieldDefinitions(typeDefinitionRegistry);
            graphQLQueryRoot.addRuntimeWiring(runtimeWiringBuilder);

            SchemaGenerator schemaGenerator = new SchemaGenerator();
            return schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiringBuilder.build());
        } catch (Exception e) {
            throw new GraphQLServiceException("An error occurred when trying to build GraphQL schema.", e);
        }
    }

    /**
     * Create the GraphQL root object which is the base of all queries.  It's children would be the folders under /conf.
     * @param resolver A live resource resolver to use to fetch resources.
     * @return The root GraphQL object.
     */
    private GraphQLQueryRoot getGraphQLQueryRoot(@NotNull final ResourceResolver resolver) {
        return ContentFragmentUtils.createModel(modelFactory, resolver.getResource(CONF_PATH), GraphQLQueryRoot.class);
    }

    /**
     * Convert a list of attributes into a GraphQL query string.
     * @param attributeList A list of desired attributes to return from the query.  Child attributes should be separated by a period.
     * @return The query part of a GraphQL query string for the desired attributes.
     */
    private static String buildAttributesFromList(List<String> attributeList) {
        Map<String, Object> fields = parseAttributeList(attributeList, new HashMap<>());
        return writeAttributeString(fields);
    }

    /**
     * Parse the linear attributes list so that those using a period to denote a child attribute are moved to a child map.
     * Leaf scalar attributes will have a null value.
     * For example passing in this for attributeList
     *  ['root.foo1', 'root.foo1.bar1', 'root.foo2']
     *  will return
     *  [root:[foo1:[bar1:null], foo2:null]]
     * @param attributeList The list of attributes to query.
     * @param attributeMap The current map of attributes (used for recursion so initially would be empty map).
     * @return A map representing the list of passed in attributes.
     */
    private static Map<String, Object> parseAttributeList(List<String> attributeList, Map<String, Object> attributeMap) {
        for (String attribute : attributeList) {
            final String[] attributeParts = attribute.split("\\.");
            if (attributeParts.length == 1) {
                attributeMap.put(attributeParts[0], null);
            } else if (attributeParts.length > 1) {
                StringBuilder referencedAttribute = new StringBuilder();
                for (int i = 1; i < attributeParts.length; i++) {
                    referencedAttribute.append(i == 1 ? "" : ".").append(attributeParts[i]);
                }
                Map<String, Object> map;
                if (attributeMap.get(attributeParts[0]) == null) {
                    map = parseAttributeList(ImmutableList.of(referencedAttribute.toString()), new HashMap<>());
                } else {
                    map = parseAttributeList(
                        ImmutableList.of(referencedAttribute.toString()),
                        (Map<String, Object>) attributeMap.get(attributeParts[0]));
                }
                attributeMap.put(attributeParts[0], map);
            }
        }
        return attributeMap;
    }

    /**
     * Convert a map of attributes to a GraphQL query string format.
     * @param attributeMap The map of attributes.
     * @return The query part of a GraphQL query string for the desired attributes.
     */
    private static String writeAttributeString(Map<String, Object> attributeMap) {
        StringBuilder fieldString = new StringBuilder();
        for (String key : attributeMap.keySet()) {
            if (attributeMap.get(key) == null) {
                fieldString.append(key).append(" ");
            } else {
                fieldString.append(key).append(" { ").append(writeAttributeString((Map<String, Object>) attributeMap.get(key))).append("} ");
            }
        }
        return fieldString.toString();
    }

    /**
     * Get a resource resolver using a designated system user so that conf folders and Content Fragment Models can be queried
     * even if they don't have anonymous access.
     * @return A Service resource resolver.
     */
    private ResourceResolver getCfManagementResourceResolver() {
        // TODO: Change to a custom user
        try {
            return resolverFactory.getServiceResourceResolver(
                    ImmutableMap.of(ResourceResolverFactory.SUBSERVICE, "cf-management"));
        } catch (LoginException le) {
            throw new GraphQLServiceException("GraphQLService cannot be run because the system cannot log in with the appropriate service user.", le);
        }
    }

    /**
     * The OSGI Config for this GraphQL Service.
     */
    @ObjectClassDefinition(name = "GraphQL Service")
    @interface Config {
        @AttributeDefinition(
                name = "Document Cache max size.",
                description = "The maximum number of GraphQL parsed query documents",
                type = AttributeType.LONG
        )
        long documentCacheMaxSize() default DEFAULT_DOCUMENT_CACHE_MAX_SIZE;

        @AttributeDefinition(
                name = "Document Cache Expiration.",
                description = "The number of minutes since the last update of the document cache before reloading it",
                type = AttributeType.LONG
        )
        long documentCacheExpire() default DEFAULT_DOCUMENT_CACHE_EXPIRE;

        @AttributeDefinition(
                name = "GraphQL Schema Cache Expiration.",
                description = "The number of minutes since the last update of the schema cache before reloading it",
                type = AttributeType.LONG
        )
        long graphQLSchemaCacheExpire() default DEFAULT_GRAPHQL_SCHEMA_CACHE_EXPIRE;

        @AttributeDefinition(
            name = "GraphQL Query Result Cache Max Size.",
            description = "The maximum number of GraphQL query results",
            type = AttributeType.LONG
        )
        long queryResultCacheMaxSize() default DEFAULT_QUERY_RESULT_CACHE_MAX_SIZE;

        @AttributeDefinition(
            name = "GraphQL Query Result Cache Expiration.",
            description = "The number of minutes since the last update of the GraphQL query results before reloading it",
            type = AttributeType.LONG
        )
        long queryResultCacheExpire() default DEFAULT_QUERY_RESULT_CACHE_EXPIRE;
    }

}
