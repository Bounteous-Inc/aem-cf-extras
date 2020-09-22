package com.bounteous.core.models.graphql.fieldmodels.cfx;

import com.bounteous.core.models.graphql.fieldmodels.GraphQLContentFragmentField;

import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.schema.DataFetcher;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        resourceType = {GraphQlCfxTabPlaceholderField.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GraphQlCfxTabPlaceholderField implements GraphQLContentFragmentField {
    static final String RESOURCE_TYPE = "settings/dam/cfm/models/formbuilderconfig/datatypes/items/cfx-tab-placeholder";

    @Inject
    @Default(values = "")
    private String name;

    @Inject
    private boolean multiple;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isMultiValue() {
        return multiple;
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
        return false;
    }

    @Override
    public List<InputValueDefinition> getInputValueDefinitions() {
        // CPD-OFF
        return new ArrayList<>();
        // CPD-ON
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
