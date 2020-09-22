package com.bounteous.core.models.graphql;

import com.bounteous.core.utils.ContentFragmentUtils;
import com.bounteous.core.utils.GraphQLUtils;

import com.drew.lang.annotations.NotNull;
import com.google.common.collect.Lists;
import graphql.execution.DataFetcherResult;
import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeName;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.factory.ModelFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

/**
 * From a Content Fragment perspective, this class represents the `/conf` directory in the repository.
 * From a GraphQL perspective it represents the root `Query` type.
 */
@Model(adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class GraphQLQueryRoot extends GraphQLBase {

    private static final String QUERY_GRAPHQL_TYPE = "Query";
    private static final String NAMESPACE_MODELS_PATH = "settings/dam/cfm/models";

    @Inject
    @Self
    private Resource resource;

    private List<GraphQLConfigNamespace> namespaces;

    @Inject
    public ModelFactory modelFactory;

    /**
     * Add the root (Query) and all configuration namespaces to the schema.
     * @param typeDefinitionRegistry a registry to add field definitions to
     */
    @Override
    public void addFieldDefinitions(@NotNull final  TypeDefinitionRegistry typeDefinitionRegistry) {
        ObjectTypeDefinition.Builder queryObjectBuilder = ObjectTypeDefinition.newObjectTypeDefinition().name("Query");

        for (GraphQLConfigNamespace graphQLConfigNamespace: getNamespaces()) {
            String namespaceType = graphQLConfigNamespace.getGraphQLType();

            queryObjectBuilder.fieldDefinition(FieldDefinition.newFieldDefinition()
                    .name(GraphQLUtils.sanitizeForVariable(namespaceType))
                    .type(new TypeName(namespaceType))
                    .build());

            // Move on to adding CF model definitions to schema
            graphQLConfigNamespace.addFieldDefinitions(typeDefinitionRegistry);
        }

        typeDefinitionRegistry.add(queryObjectBuilder.build());
    }

    /**
     * Define queryable attributes at the configuration namespace level.  Currently there are none.
     * @return a lit of queryable fields
     */
    @Override
    public List<InputValueDefinition> getInputValueDefinitions() {
        return Collections.emptyList();
    }

    /**
     * This is the name of the root Query object in the GraphQL Schema.
     * @return always "Query" for this class
     */
    @Override
    public String getGraphQLType() {
        return QUERY_GRAPHQL_TYPE;
    }

    /**
     * Grab all the configuration folder under /conf that contain Content Fragment Models and return them as GraphQLConfigNamespace objects.
     * @return a list of config namespace objects
     */
    public List<GraphQLConfigNamespace> getNamespaces() {
        if (namespaces == null) {
            namespaces = Lists.newArrayList(resource.getChildren()).stream()
                    .filter(this::hasModels)
                    .map(resource -> (GraphQLConfigNamespace)ContentFragmentUtils.createModel(modelFactory, resource, GraphQLConfigNamespace.class))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return namespaces;
    }

    /**
     * Get a specific configuration namespace based on a given name.
     * @param name the name of the configuration to return
     * @return the config namespace object
     */
    public GraphQLConfigNamespace getNamespace(@NotNull final String name) {
        Optional<GraphQLConfigNamespace> namespace = getNamespaces().stream()
                .filter(currentNamespace -> currentNamespace.getGraphQLName().equalsIgnoreCase(name))
                .findFirst();

        return namespace.orElse(null);
    }

    /**
     * We need to make sure the "namespace" configuration folder actually has a `models` folder, indicating it is configured
     * for content fragment models, before we add this namespace to the schema.
     * For example, the `/conf/screens` folder out of box does not have a `models` folder.
     * @param namespaceFolderResource an aem configuration resource
     * @return true if this namespace folder is configured for content fragment models
     */
    private boolean hasModels(@NotNull final Resource namespaceFolderResource) {
        Resource namespaceModelsResource = namespaceFolderResource.getChild(NAMESPACE_MODELS_PATH);
        //TODO: Check that the children in the models path are valid models
        return namespaceModelsResource != null && namespaceModelsResource.hasChildren();
    }

    @Override
    public void addRuntimeWiring(@NotNull final RuntimeWiring.Builder runtimeWiringBuilder) {
        for (GraphQLConfigNamespace graphQLConfigNamespace : getNamespaces()) {
            runtimeWiringBuilder
                    .type(newTypeWiring(getGraphQLType())
                            .dataFetcher(GraphQLUtils.sanitizeForVariable(graphQLConfigNamespace.getGraphQLType()), buildDataFetcher()));

            graphQLConfigNamespace.addRuntimeWiring(runtimeWiringBuilder);
        }
    }

    /**
     * Produce a Map representing a config namespace and it's attributes with
     * This is used during the execution of a GraphQL Query.
     * @return namespace Map
     */
    @Override
    protected DataFetcher buildDataFetcher() {
        return dataFetchingEnvironment -> {
            GraphQLOutputType graphQLOutputType = dataFetchingEnvironment.getFieldType();
            List<GraphQLType> fields = graphQLOutputType.getChildren();
            GraphQLBase.DataFetcherContext localContext = dataFetchingEnvironment.getLocalContext();
            GraphQLQueryRoot source = dataFetchingEnvironment.getSource();

            Map<String,Object> resultMap = fields.stream()
                    .collect(Collectors.toMap(field -> GraphQLUtils.sanitizeForVariable(field.getName()), graphQLType -> Collections.emptyList()));

            String fieldName = dataFetchingEnvironment.getFieldDefinition().getName();
            GraphQLConfigNamespace namespace = source.getNamespace(fieldName);

            return DataFetcherResult.newResult()
                    .data(resultMap)
                    .localContext(new GraphQLBase.DataFetcherContext(localContext.getResourceResolver(), namespace))
                    .build();
        };
    }

}
