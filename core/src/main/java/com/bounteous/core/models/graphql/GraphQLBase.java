package com.bounteous.core.models.graphql;

import com.bounteous.core.utils.GraphQLUtils;

import com.drew.lang.annotations.NotNull;
import com.google.common.collect.ImmutableList;
import graphql.language.InputValueDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

public abstract class GraphQLBase {

    public static final String ID_ATTRIBUTE = "id";
    protected static final String GRAPHQL_FIELD_PATTERN = "[_A-Za-z][_0-9A-Za-z]*";
    protected static final String BY_ID_SUFFIX = "ById";
    protected static final String LIST_SUFFIX = "List";

    protected static final List<String> EXCLUDED_CFM_FIELD_TYPES = ImmutableList.of(
            "aem-cf-extras/cfm/models/datatypeproperties/cfx-tab-placeholder"
    );

    public abstract void addFieldDefinitions(@NotNull final TypeDefinitionRegistry typeDefinitionRegistry);

    public abstract void addRuntimeWiring(@NotNull final RuntimeWiring.Builder runtimeWiringBuilder);

    public abstract List<InputValueDefinition> getInputValueDefinitions();

    public abstract String getGraphQLType();

    protected String cfmTypeToListAccessorMethod(@NotNull final String cfmType) {
        return GraphQLUtils.sanitizeForVariable(cfmType) + LIST_SUFFIX;
    }

    protected String cfmTypeToByIdAccessorMethod(@NotNull final String cfmType) {
        return GraphQLUtils.sanitizeForVariable(cfmType) + BY_ID_SUFFIX;
    }

    protected abstract DataFetcher buildDataFetcher();

    public static class DataFetcherContext {

        private final ResourceResolver resourceResolver;
        private final GraphQLBase slingModel;

        public DataFetcherContext(@NotNull final ResourceResolver resourceResolver) {
            this(resourceResolver, null);
        }

        public DataFetcherContext(@NotNull final ResourceResolver resourceResolver, @NotNull final GraphQLBase slingModel) {
            this.resourceResolver = resourceResolver;
            this.slingModel = slingModel;
        }

        public ResourceResolver getResourceResolver() {
            return resourceResolver;
        }

        public GraphQLBase getSlingModel() {
            return slingModel;
        }
    }
}
