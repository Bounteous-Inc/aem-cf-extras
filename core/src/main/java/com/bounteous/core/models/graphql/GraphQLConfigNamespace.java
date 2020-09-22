package com.bounteous.core.models.graphql;

import com.bounteous.core.utils.ContentFragmentUtils;
import com.bounteous.core.utils.GraphQLUtils;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.FragmentData;
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
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.factory.ModelFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.day.crx.JcrConstants.JCR_CONTENT;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

/**
 * From a Content Fragment perspective, this class represents an AEM Configuration definition under `/conf`.
 * From a GraphQL perspective it represents a query path used to query a specific set of Content Fragment Models
 * located under the previously mentioned AEM Configuration.
 * A node under `/conf` is considered to be a valid "namespace" if it contains any Content Fragment Models under the
 * relative path `settings/dam/cfm/models`.
 */
@Model(adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class GraphQLConfigNamespace extends GraphQLBase {

    private static final String MODELS_PATH = "settings/dam/cfm/models";

    @Inject
    @Self
    private Resource resource;

    @Inject
    public ModelFactory modelFactory;

    private List<GraphQLContentFragmentModel> contentFragmentModels;

    public String getName() {
        return resource.getName();
    }

    public String getGraphQLName() {
        return GraphQLUtils.sanitizeForVariable(getName());
    }

    public String getGraphQLType() {
        return GraphQLUtils.sanitizeForType(getName());
    }

    public List<GraphQLContentFragmentModel> getContentFragmentModels() {
        if (contentFragmentModels == null) {
            Resource namespaceModelsResource = this.resource.getChild(MODELS_PATH);
            contentFragmentModels = namespaceModelsResource != null ?
                Lists.newArrayList(namespaceModelsResource.getChildren()).stream()
                    .map(resource -> resource.getName().equals(JCR_CONTENT) ? resource : resource.getChild(JCR_CONTENT))
                    .filter(Objects::nonNull)
                    .map(resource ->
                        (GraphQLContentFragmentModel) ContentFragmentUtils.createModel(modelFactory, resource, GraphQLContentFragmentModel.class)
                    )
                    .filter(Objects::nonNull)
                    .filter(GraphQLContentFragmentModel::isValid)
                    .collect(Collectors.toList()) :
                Collections.emptyList();
        }

        return contentFragmentModels;
    }

    public GraphQLContentFragmentModel getContentFragmentModel(@NotNull final String modelType) {
        Optional<GraphQLContentFragmentModel> cfm = getContentFragmentModels().stream()
                .filter(currentModel -> currentModel.getGraphQLType().equalsIgnoreCase(modelType))
                .findFirst();

        return cfm.orElse(null);
    }

    public ConfigNamespaceInfo getConfigNamespaceInfo() {
        return new GraphQLConfigNamespace.ConfigNamespaceInfo(
            getName(),
            getGraphQLName()
        );
    }

    @Override
    public List<InputValueDefinition> getInputValueDefinitions() {
        return null;
    }

    @Override
    public void addFieldDefinitions(@NotNull final TypeDefinitionRegistry typeDefinitionRegistry) {
        ObjectTypeDefinition.Builder namespaceObjectBuilder = ObjectTypeDefinition.newObjectTypeDefinition()
                .name(getGraphQLType());

        final List<GraphQLContentFragmentModel> cfmList = getContentFragmentModels();

        List<FieldDefinition> fieldsList = new ArrayList<>();

        for (GraphQLContentFragmentModel cfm : cfmList) {
            String cfmType = GraphQLUtils.sanitizeForType(cfm.getName());
            String cfmListVariable = cfmTypeToListAccessorMethod(cfm.getGraphQLType());
            String cfmByIdVariable = cfmTypeToByIdAccessorMethod(cfm.getGraphQLType());

            fieldsList.add(FieldDefinition.newFieldDefinition()
                    .name(cfmListVariable)
                    .type(new ListType(new TypeName(cfmType)))
                    .inputValueDefinitions(cfm.getInputValueDefinitions())
                    .build());

            fieldsList.add(FieldDefinition.newFieldDefinition()
                    .name(cfmByIdVariable)
                    .type(new TypeName(cfmType))
                    .inputValueDefinition(new InputValueDefinition(ID_ATTRIBUTE, new TypeName(Scalars.GraphQLID.getName())))
                    .build());

            //TODO: Modularize these
            cfm.addFieldDefinitions(typeDefinitionRegistry);
            cfm.addTagDefinitions(typeDefinitionRegistry);
            cfm.addDateTimeDefinitions(typeDefinitionRegistry);
            cfm.addDateDefinitions(typeDefinitionRegistry);
            cfm.addTimeDefinitions(typeDefinitionRegistry);
        }

        namespaceObjectBuilder.fieldDefinitions(fieldsList);

        typeDefinitionRegistry.add(namespaceObjectBuilder.build());
    }

    @Override
    public void addRuntimeWiring(@NotNull final RuntimeWiring.Builder runtimeWiringBuilder) {
        final List<GraphQLContentFragmentModel> cfmList = getContentFragmentModels();

        runtimeWiringBuilder
                .type(newTypeWiring(getGraphQLType())
                        .dataFetcher(ID_ATTRIBUTE, buildDataFetcher()));

        for (GraphQLContentFragmentModel cfm : cfmList) {
            String cfmListVariable = cfmTypeToListAccessorMethod(cfm.getName());
            String cfmByIdVariable = cfmTypeToByIdAccessorMethod(cfm.getName());

            runtimeWiringBuilder
                .type(newTypeWiring(getGraphQLType())
                    .dataFetcher(cfmListVariable, buildDataFetcher()));

            runtimeWiringBuilder
                .type(newTypeWiring(getGraphQLType())
                    .dataFetcher(cfmByIdVariable, buildDataFetcher()));

            cfm.addRuntimeWiring(runtimeWiringBuilder);
        }
    }

    /**
     * Produce a List of content fragments based on a given content fragment model.
     * @return
     */
    @Override
    protected DataFetcher buildDataFetcher() {
        return dataFetchingEnvironment -> {
            List<Map<String, Object>> resultList;

            final Field field = dataFetchingEnvironment.getField();
            final Map<String,Object> args = dataFetchingEnvironment.getArguments();
            final String idArg = (String) args.get(ID_ATTRIBUTE);
            final boolean searchById = StringUtils.isNotBlank(idArg) && field.getName().endsWith(BY_ID_SUFFIX);
            final GraphQLOutputType graphQLOutputType = dataFetchingEnvironment.getFieldType();

            final DataFetcherContext localContext = dataFetchingEnvironment.getLocalContext();
            final GraphQLConfigNamespace graphQLConfigNamespace = (GraphQLConfigNamespace) localContext.getSlingModel();

            String cfModelType;
            if (searchById) {
                cfModelType = graphQLOutputType.getName();
            } else {
                // Need to extract type of array (i.e. [Property] -> Property)
                cfModelType = Objects.requireNonNull(graphQLOutputType.getChildren().stream().findFirst().orElse(null)).getName();
            }

            GraphQLContentFragmentModel cfm = graphQLConfigNamespace.getContentFragmentModel(cfModelType);
            List<GraphQLContentFragment> contentFragments = cfm.findContentFragmentsForTemplate(0, 1000, modelFactory);

            resultList = contentFragments.stream()
                    .filter(cf -> args.keySet().size() == 0 || args.keySet().stream().allMatch(key -> {
                        if (key.equals(ID_ATTRIBUTE) && searchById) {
                            return cf.getId().equals(idArg);
                        }

                        FragmentData fieldValue = cf.getElement(key).getValue();
                        List argMatchList = (List) args.get(key);

                        return fieldValue != null &&
                                fieldValue.getValue(String.class) != null &&
                                argMatchList.contains(fieldValue.getValue(String.class));
                    }))
                    .map(cf -> {
                        Map<String, Object> fieldMap = Lists.newArrayList(cf.getElements()).stream()
                                //.filter(cfElement -> cf.getFieldValue(cfElement) != null)
                                .collect(Collectors.toMap(ContentElement::getName, cfElement -> cfElement));

                        fieldMap.put(ID_ATTRIBUTE, cf.getId());

                        return fieldMap;
                    })
                    .distinct()
                    .collect(Collectors.toList());

            if (searchById) {
                return DataFetcherResult.newResult()
                    .data(resultList.size() > 0 ? resultList.get(0) : Collections.emptyMap())
                    .localContext(new GraphQLBase.DataFetcherContext(localContext.getResourceResolver()))
                    .build();
            }

            return DataFetcherResult.newResult()
                    .data(resultList)
                    .localContext(new GraphQLBase.DataFetcherContext(localContext.getResourceResolver()))
                    .build();
        };
    }

    public static class ConfigNamespaceInfo {
        private String name;
        private String graphQLName;

        public ConfigNamespaceInfo(String name, String graphQLName) {
            this.name = name;
            this.graphQLName = graphQLName;
        }

        public String getName() {
            return name;
        }

        public String getGraphQLName() {
            return graphQLName;
        }
    }

}
