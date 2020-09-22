package com.bounteous.core.models.graphql.fieldmodels.cfx;

import com.bounteous.core.models.graphql.fieldmodels.GraphQLContentFragmentField;

import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.schema.DataFetcher;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import java.util.ArrayList;
import java.util.List;

@Model(
    adaptables = { Resource.class, SlingHttpServletRequest.class },
    resourceType = {GraphQlCfxIdField.RESOURCE_TYPE},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GraphQlCfxIdField implements GraphQLContentFragmentField {
    static final String RESOURCE_TYPE = "settings/dam/cfm/models/formbuilderconfig/datatypes/items/cfx-id-field";

    @Override
    public String getName() {
        return "id";
    }

    @Override
    public boolean isMultiValue() {
        return false;
    }

    @Override
    public DataFetcher getDataFetcher() {
        return null;
    }

    @Override
    public String getGraphQLType() {
        return null;
    }

    @Override
    public boolean getProducesValue() {
        // Note that even though
        return false;
    }

    @Override
    public List<InputValueDefinition> getInputValueDefinitions() {
        return new ArrayList<>();
    }

    @Override
    public FieldDefinition getFieldDefinition() {
        return null;
    }

    @Override
    public boolean isFilterable() {
        return false;
    }
}
