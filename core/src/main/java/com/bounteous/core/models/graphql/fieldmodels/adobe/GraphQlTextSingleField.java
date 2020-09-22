package com.bounteous.core.models.graphql.fieldmodels.adobe;

import com.bounteous.core.models.graphql.fieldmodels.AbstractGraphQlDataTypeField;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;

@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        resourceType = {GraphQlTextSingleField.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GraphQlTextSingleField extends AbstractGraphQlDataTypeField {
    static final String RESOURCE_TYPE = "settings/dam/cfm/models/formbuilderconfig/datatypes/items/text-single";

    @Inject
    @Default(values = "")
    private String name;

    @Inject
    private String valueType;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isMultiValue() {
        return valueType != null && valueType.contains("[]");
    }

    @Override
    public boolean isFilterable() {
        return true;
    }

}
