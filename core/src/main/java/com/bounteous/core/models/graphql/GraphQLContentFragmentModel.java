package com.bounteous.core.models.graphql;

import com.bounteous.core.models.graphql.fieldmodels.GraphQLContentFragmentField;
import com.bounteous.core.models.graphql.fieldmodels.adobe.GraphQlDateField;
import com.bounteous.core.models.graphql.fieldmodels.adobe.GraphQlTagsField;
import com.bounteous.core.models.graphql.fieldmodels.cfx.GraphQlCfxDateField;
import com.bounteous.core.services.impl.GraphQLServiceImpl;
import com.bounteous.core.utils.ContentFragmentUtils;
import com.bounteous.core.utils.GraphQLUtils;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.adobe.cq.dam.cfm.ElementTemplate;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.adobe.cq.dam.cfm.MetaDataDefinition;
import com.adobe.cq.dam.cfm.VariationTemplate;
import com.drew.lang.annotations.NotNull;
import com.google.common.collect.Lists;
import graphql.Scalars;
import graphql.execution.DataFetcherResult;
import graphql.language.Field;
import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.ListType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeName;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLOutputType;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.factory.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bounteous.core.utils.GraphQLUtils.getSlingModelClassForField;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

/**
 * This class is a special implementation of the Adobe FragmentTemplate interface.
 * It uses a decorator pattern, keeping an instance of an adapted FragmentTemplates in the member variable `contentFragment`.
 * This allows it to take advantage of all the methods already implemented by Adobe but extend that by adding it's own.
 * Also the naming convention was changed to make it more clear what this represents from a structured content fragment context.
 */
@Model(adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class GraphQLContentFragmentModel extends GraphQLBase implements FragmentTemplate {
    private static final Logger LOG = LoggerFactory.getLogger(GraphQLContentFragmentModel.class);

    /*
    private static final Map<String,String> CF_TYPE_TO_GRAPHQL_TYPE = ImmutableMap.of(
            BasicDataType.STRING, Scalars.GraphQLString.getName(),
            BasicDataType.CALENDAR, Scalars.GraphQLString.getName(),
            BasicDataType.BOOLEAN, Scalars.GraphQLBoolean.getName(),
            BasicDataType.LONG, Scalars.GraphQLLong.getName(),
            BasicDataType.DOUBLE, Scalars.GraphQLBigDecimal.getName()
    );
     */

    @Inject
    @Self
    private Resource resource;

    @Inject
    @Self
    private FragmentTemplate fragmentTemplate;

    @Inject
    private ModelFactory modelFactory;

    @PostConstruct
    protected void init() {
        // The call to getChild() is necessary for compatibility with the FragmentTemplate adaptTo() in AEM 6.4.
        //this.fragmentTemplate = resource.getChild(JcrConstants.JCR_CONTENT).adaptTo(FragmentTemplate.class);
    }

    public boolean isValid() {
        return fragmentTemplate != null;
    }

    public String getName() {
        return resource.getParent().getName();  // because the resource is 'jcr:content'
    }

    public String getGraphQLType() {
        return GraphQLUtils.sanitizeForType(getName());
    }

    /**
     * Provide a list queryable fields for this object.
     * @return
     */
    @Override
    public List<InputValueDefinition> getInputValueDefinitions() {
        List<InputValueDefinition> inputValueDefinitionList = Lists.newArrayList(getElements()).stream()
                .map(cfmElement -> getSlingModelClassForField(cfmElement, modelFactory))
                .filter(Objects::nonNull)
                .filter(this::isIncludedField)
                .filter(GraphQLContentFragmentField::isFilterable)
                .map(slingModelField -> new InputValueDefinition(GraphQLUtils.sanitizeForVariable(slingModelField.getName()),
                        new ListType(new TypeName(slingModelField.getGraphQLType()))))
                .collect(Collectors.toList());

        return inputValueDefinitionList;
    }

    @Override
    public void addFieldDefinitions(@NotNull final TypeDefinitionRegistry typeDefinitionRegistry) {
        ObjectTypeDefinition.Builder cfmObjectBuilder = ObjectTypeDefinition.newObjectTypeDefinition().name(getGraphQLType());

        FieldDefinition cfmIdField = FieldDefinition.newFieldDefinition()
                .name(ID_ATTRIBUTE)
                .type(new TypeName(Scalars.GraphQLID.getName()))
                .build();

        List<FieldDefinition> fieldsList = Lists.newArrayList(getElements()).stream()
                .filter(this::isIncludedField)
                .map(cfmElement -> {
                    GraphQLContentFragmentField cfField = getSlingModelClassForField(cfmElement, modelFactory);
                    if (cfField != null && cfField.getProducesValue()) {
                        return cfField.getFieldDefinition();
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        fieldsList.add(cfmIdField);
        cfmObjectBuilder.fieldDefinitions(fieldsList);

        typeDefinitionRegistry.add(cfmObjectBuilder.build());
    }

    public void addTagDefinitions(@NotNull final TypeDefinitionRegistry typeDefinitionRegistry) {
        ObjectTypeDefinition.Builder cfmObjectBuilder = ObjectTypeDefinition.newObjectTypeDefinition().name(GraphQlTagsField.TAG_GRAPHQL_TYPE);

        FieldDefinition cfmIdField = FieldDefinition.newFieldDefinition()
                .name(ID_ATTRIBUTE)
                .type(new TypeName(Scalars.GraphQLID.getName()))
                .build();

        FieldDefinition cfmNameField = FieldDefinition.newFieldDefinition()
                .name(GraphQlTagsField.NAME)
                .type(new TypeName(Scalars.GraphQLString.getName()))
                .build();

        FieldDefinition cfmTitleField = FieldDefinition.newFieldDefinition()
                .name(GraphQlTagsField.TITLE)
                .type(new TypeName(Scalars.GraphQLString.getName()))
                .build();

        FieldDefinition cfmDescriptionField = FieldDefinition.newFieldDefinition()
            .name(GraphQlTagsField.DESCRIPTION)
            .type(new TypeName(Scalars.GraphQLString.getName()))
            .build();

        List<FieldDefinition> fieldsList = new ArrayList<>();
        fieldsList.add(cfmIdField);
        fieldsList.add(cfmNameField);
        fieldsList.add(cfmTitleField);
        fieldsList.add(cfmDescriptionField);
        cfmObjectBuilder.fieldDefinitions(fieldsList);

        typeDefinitionRegistry.add(cfmObjectBuilder.build());
    }

    public void addDateTimeDefinitions(@NotNull final TypeDefinitionRegistry typeDefinitionRegistry) {
        ObjectTypeDefinition.Builder cfmObjectBuilder = ObjectTypeDefinition.newObjectTypeDefinition().name(GraphQlDateField.DATETIME_GRAPHQL_TYPE);

        List<FieldDefinition> fieldsList = new ArrayList<>(getCommonDateTimeFields());
        fieldsList.addAll(getDateFields());
        fieldsList.addAll(getTimeFields());
        cfmObjectBuilder.fieldDefinitions(fieldsList);

        typeDefinitionRegistry.add(cfmObjectBuilder.build());
    }

    public void addDateDefinitions(@NotNull final TypeDefinitionRegistry typeDefinitionRegistry) {
        ObjectTypeDefinition.Builder cfmObjectBuilder = ObjectTypeDefinition.newObjectTypeDefinition().name(GraphQlCfxDateField.DATE_GRAPHQL_TYPE);

        List<FieldDefinition> fieldsList = new ArrayList<>(getCommonDateTimeFields());
        fieldsList.addAll(getDateFields());
        cfmObjectBuilder.fieldDefinitions(fieldsList);

        typeDefinitionRegistry.add(cfmObjectBuilder.build());
    }

    public void addTimeDefinitions(@NotNull final TypeDefinitionRegistry typeDefinitionRegistry) {
        ObjectTypeDefinition.Builder cfmObjectBuilder = ObjectTypeDefinition.newObjectTypeDefinition().name(GraphQlCfxDateField.TIME_GRAPHQL_TYPE);

        List<FieldDefinition> fieldsList = new ArrayList<>(getCommonDateTimeFields());
        fieldsList.addAll(getTimeFields());
        cfmObjectBuilder.fieldDefinitions(fieldsList);

        typeDefinitionRegistry.add(cfmObjectBuilder.build());
    }

    private List<FieldDefinition> getCommonDateTimeFields() {
        FieldDefinition cfmFormattedField = FieldDefinition.newFieldDefinition()
            .name(GraphQlDateField.FORMATTED)
            .type(new TypeName(Scalars.GraphQLString.getName()))
            .build();

        List<FieldDefinition> fieldsList = new ArrayList<>();
        fieldsList.add(cfmFormattedField);

        return fieldsList;
    }

    private List<FieldDefinition> getDateFields() {
        FieldDefinition cfmYearField = FieldDefinition.newFieldDefinition()
            .name(GraphQlDateField.YEAR)
            .type(new TypeName(Scalars.GraphQLLong.getName()))
            .build();

        FieldDefinition cfmMonthField = FieldDefinition.newFieldDefinition()
            .name(GraphQlDateField.MONTH)
            .type(new TypeName(Scalars.GraphQLLong.getName()))
            .build();

        FieldDefinition cfmDayField = FieldDefinition.newFieldDefinition()
            .name(GraphQlDateField.DAY)
            .type(new TypeName(Scalars.GraphQLLong.getName()))
            .build();

        FieldDefinition cfmTimestampField = FieldDefinition.newFieldDefinition()
            .name(GraphQlDateField.TIMESTAMP)
            .type(new TypeName(Scalars.GraphQLLong.getName()))
            .build();

        List<FieldDefinition> fieldsList = new ArrayList<>();
        fieldsList.add(cfmYearField);
        fieldsList.add(cfmMonthField);
        fieldsList.add(cfmDayField);
        fieldsList.add(cfmTimestampField);

        return fieldsList;
    }

    private List<FieldDefinition> getTimeFields() {
        FieldDefinition cfmHourField = FieldDefinition.newFieldDefinition()
            .name(GraphQlDateField.HOUR)
            .type(new TypeName(Scalars.GraphQLLong.getName()))
            .build();

        FieldDefinition cfmMinuteField = FieldDefinition.newFieldDefinition()
            .name(GraphQlDateField.MINUTE)
            .type(new TypeName(Scalars.GraphQLLong.getName()))
            .build();

        FieldDefinition cfmSecondField = FieldDefinition.newFieldDefinition()
            .name(GraphQlDateField.SECOND)
            .type(new TypeName(Scalars.GraphQLLong.getName()))
            .build();

        List<FieldDefinition> fieldsList = new ArrayList<>();
        fieldsList.add(cfmHourField);
        fieldsList.add(cfmMinuteField);
        fieldsList.add(cfmSecondField);

        return fieldsList;
    }

    @Override
    public void addRuntimeWiring(@NotNull final  RuntimeWiring.Builder runtimeWiringBuilder) {
        // TODO:  For each ElementTemplate (CFM field) use 'metaType' attribute to construct field resource type
        //        `settings/dam/cfm/models/formbuilderconfig/datatypes/items/<metaType>`.
        //        Then use ModelFactory.getModelFromResource() to get a corresponding Sling Model class.
        //        Finally, adapt ElementTemplate to Resource and adapt that to the Sling Model class.
        //        Potentially check that the Sling Model class extends GraphQLContentFragmentField.
        Iterator<ElementTemplate> elements = getElements();
        List<ElementTemplate> elementList = Lists.newArrayList(elements);

        Map<String, DataFetcher> dataFetcherMap = elementList.stream()
                .map(cfmElement -> getSlingModelClassForField(cfmElement, modelFactory))
                .filter(Objects::nonNull)
                .filter(this::isIncludedField)
                .collect(Collectors.toMap(GraphQLContentFragmentField::getName, GraphQLContentFragmentField::getDataFetcher));

        dataFetcherMap.put(ID_ATTRIBUTE, buildDataFetcher());

        runtimeWiringBuilder
                .type(newTypeWiring(getGraphQLType())
                .dataFetchers(dataFetcherMap));

    }

    public ContentFragmentModelInfo getContentFragmentModelInfo() {
        GraphQLConfigNamespace.ConfigNamespaceInfo configNamespaceInfo = null;

        //TODO: Should be able to find the /conf folder resource with Sling API instead
        Resource currentResource = this.resource;
        while (currentResource != null) {
            Resource parentResource = currentResource.getParent();
            if (parentResource != null && parentResource.getPath().equals(GraphQLServiceImpl.CONF_PATH)) {
                GraphQLConfigNamespace graphQLConfigNamespace = currentResource.adaptTo(GraphQLConfigNamespace.class);
                if (graphQLConfigNamespace != null) {
                    configNamespaceInfo = graphQLConfigNamespace.getConfigNamespaceInfo();
                    break;
                }
            }
            currentResource = parentResource;
        }

        return new ContentFragmentModelInfo(
            getName(),
            this.resource.getPath(),
            configNamespaceInfo
        );
    }

    public DataFetcher buildIdDataFetcher() {
        return dataFetchingEnvironment -> {
            final GraphQLOutputType type = dataFetchingEnvironment.getFieldType();
            if (type.getName().equals(Scalars.GraphQLID.getName())) {
                return null;
            }

            return null;
        };
    }

    /**
     * The job of the builder method is to recognize fields in the source Map that are reference paths and convert their
     * values from a String path to the appropriate GraphQL object.
     * @return
     */
    @Override
    protected DataFetcher buildDataFetcher() {
        return dataFetchingEnvironment -> {
            final Field field = dataFetchingEnvironment.getField();

            Map<String,Object> sourceMap = dataFetchingEnvironment.getSource();

            //TODO: We can assume that the field is a reference field because addRuntimeWiring() checked, but need a way
            // to register and identify custom reference fields and a modular way to handle them.  Maybe a List of
            // handlers in the context.  These handlers classes can be named in an OSGI config.
            Object cfField = sourceMap.get(field.getName());

            if (cfField != null) {
                final DataFetcherContext localContext = dataFetchingEnvironment.getLocalContext();
                final ResourceResolver resourceResolver = localContext.getResourceResolver();

                if (cfField instanceof String[]) {
                    List<Map<String, Object>> cfFieldList = Arrays.stream((String[]) cfField)
                        .filter(Objects::nonNull)
                        .filter(StringUtils::isNotEmpty)
                        .map(resourceResolver::getResource)
                        .filter(Objects::nonNull)
                        .map(resource -> ContentFragmentUtils.createModel(modelFactory, resource, GraphQLContentFragment.class))
                        .filter(Objects::nonNull)
                        .filter(GraphQLContentFragment::isValid)
                        .map(GraphQLContentFragment::toMap)
                        .collect(Collectors.toList());

                    return DataFetcherResult.newResult()
                        .data(cfFieldList)
                        .build();
                } else {
                    String cfPath = cfField.toString();
                    if (StringUtils.isNotEmpty(cfPath)) {
                        Resource cfResource = resourceResolver.getResource(cfPath);
                        if (cfResource != null) {
                            GraphQLContentFragment cf = cfResource.adaptTo(GraphQLContentFragment.class);
                            if (cf != null) {
                                Map<String,Object> cfMap = cf.toMap();

                                return DataFetcherResult.newResult()
                                        .data(cfMap)
                                        .build();
                            }
                        }
                    }
                }
            }

            return DataFetcherResult.newResult()
                    .data(cfField)
                    .build();
        };
    }

    private boolean isIncludedField(@NotNull final ElementTemplate fieldElement) {
        // GraphQL does not allow field names that start with a number
        if (!fieldElement.getName().matches(GRAPHQL_FIELD_PATTERN)) {
            return false;
        }

        Resource elementResource = fieldElement.adaptTo(Resource.class);
        if (elementResource != null) {
            // TODO: Check to see if the field resource adapts to a Sling Model with the given resource type
            return !EXCLUDED_CFM_FIELD_TYPES.contains(elementResource.getResourceType());
        }
        return false;
    }

    private boolean isIncludedField(@NotNull final GraphQLContentFragmentField cfField) {
        // GraphQL does not allow field names that start with a number
        DataFetcher dataFetcher = cfField.getDataFetcher();
        if (!cfField.getName().matches(GRAPHQL_FIELD_PATTERN) || dataFetcher == null) {
            return false;
        }

        return cfField.getProducesValue();
    }

    //TODO: cache search results
    public List<GraphQLContentFragment> findContentFragmentsForTemplate(final int offset, final int limit, ModelFactory modelFactory) {
        List<GraphQLContentFragment> contentFragments = new ArrayList<>();

        final ResourceResolver resourceResolver = resource.getResourceResolver();
        final Resource fragmentTemplateResource = fragmentTemplate.adaptTo(Resource.class);
        if (fragmentTemplateResource != null) {
            final Resource templateParentResource = fragmentTemplateResource.getParent();
            if (templateParentResource != null) {
                final String fragmentPath = templateParentResource.getPath();

                try {
                    Session session = resourceResolver.adaptTo(Session.class);
                    if (session != null) {
                        Query query = session.getWorkspace().getQueryManager()
                            .createQuery(
                                String.format("/jcr:root/content/dam//element(*, dam:Asset)[jcr:content/data/@cq:model='%s']", fragmentPath),
                                "xpath");
                        query.setOffset(offset);
                        query.setLimit(limit);
                        QueryResult queryResult = query.execute();
                        for (NodeIterator nodes = queryResult.getNodes(); nodes.hasNext(); ) {
                            Node node = nodes.nextNode();
                            Resource resource = resourceResolver.getResource(node.getPath());
                            GraphQLContentFragment graphQLContentFragment =
                                ContentFragmentUtils.createModel(modelFactory, resource, GraphQLContentFragment.class);
                            if (graphQLContentFragment != null) {
                                contentFragments.add(graphQLContentFragment);
                            }
                        }
                    }
                } catch (RepositoryException e) {
                    LOG.error("[findContentFragmentsForTemplate] failed to execute cf query from template " + fragmentPath, e);
                }
            }
        }

        return contentFragments;
    }

    // Below are all methods that must be implemented from the FragmentTemplate interface

    @Override
    public String getTitle() {
        return fragmentTemplate.getTitle();
    }

    @Override
    public String getDescription() {
        return fragmentTemplate.getDescription();
    }

    @Override
    public String getThumbnailPath() {
        return fragmentTemplate.getThumbnailPath();
    }

    @Override
    public ContentFragment createFragment(Resource resource, String s, String s1) throws ContentFragmentException {
        return fragmentTemplate.createFragment(resource, s, s1);
    }

    @Override
    public Iterator<ElementTemplate> getElements() {
        return fragmentTemplate.getElements();
    }

    @Override
    public ElementTemplate getForElement(ContentElement contentElement) {
        return fragmentTemplate.getForElement(contentElement);
    }

    @Override
    public Iterator<VariationTemplate> getVariations() {
        return fragmentTemplate.getVariations();
    }

    @Override
    public VariationTemplate getForVariation(ContentVariation contentVariation) {
        return fragmentTemplate.getForVariation(contentVariation);
    }

    @Override
    public Iterator<String> getInitialAssociatedContent() {
        return fragmentTemplate.getInitialAssociatedContent();
    }

    @Override
    public MetaDataDefinition getMetaDataDefinition() {
        return fragmentTemplate.getMetaDataDefinition();
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> adapterClass) {
        return fragmentTemplate.adaptTo(adapterClass);
    }

    /**
     * This class can be used to hold information about a Content Fragment without the need for an open resource resolver.
     */
    public static class ContentFragmentModelInfo {
        private final String name;
        private final String path;
        private final GraphQLConfigNamespace.ConfigNamespaceInfo configNamespaceInfo;

        public ContentFragmentModelInfo(String name, String path, GraphQLConfigNamespace.ConfigNamespaceInfo configNamespaceInfo) {
            this.name = name;
            this.path = path;
            this.configNamespaceInfo = configNamespaceInfo;
        }

        public String getName() {
            return name;
        }

        //TODO: This needs to be set properly by moving GraphQLBase.cfmTypeToByIdAccessorMethod() to GraphQLUtils and calling that from
        // GraphQLServiceImpl.executeQueryForCfPath()
        public String getGraphQLName() {
            return GraphQLUtils.sanitizeForVariable(name);
        }

        public String getPath() {
            return path;
        }

        public GraphQLConfigNamespace.ConfigNamespaceInfo getConfigNamespaceInfo() {
            return configNamespaceInfo;
        }
    }
}
