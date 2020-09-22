package com.bounteous.core.models.graphql.fieldmodels.adobe;

import com.bounteous.core.models.graphql.fieldmodels.AbstractGraphQlDataTypeField;

import graphql.Scalars;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;

@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        resourceType = {GraphQlBooleanField.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GraphQlBooleanField extends AbstractGraphQlDataTypeField {
    static final String RESOURCE_TYPE = "settings/dam/cfm/models/formbuilderconfig/datatypes/items/boolean";

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
    public String getGraphQLType() {
        return Scalars.GraphQLBoolean.getName();
    }

}
