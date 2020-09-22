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
        resourceType = {GraphQlReferenceField.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GraphQlReferenceField extends AbstractGraphQlDataTypeField {
    static final String RESOURCE_TYPE = "settings/dam/cfm/models/formbuilderconfig/datatypes/items/reference";

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
        //TODO:  This should return a new Query for the entered path assuming it points to another Content Fragment.
        //  Otherwise, if it points to something else like a page or asset it should just return the path.
        return Scalars.GraphQLString.getName();
    }

}
