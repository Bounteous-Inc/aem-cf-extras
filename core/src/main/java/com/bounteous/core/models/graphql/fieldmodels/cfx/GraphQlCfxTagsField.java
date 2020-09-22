package com.bounteous.core.models.graphql.fieldmodels.cfx;

import com.bounteous.core.models.graphql.fieldmodels.adobe.GraphQlTagsField;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        resourceType = {GraphQlCfxTagsField.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GraphQlCfxTagsField extends GraphQlTagsField {
    static final String RESOURCE_TYPE = "settings/dam/cfm/models/formbuilderconfig/datatypes/items/cfx-tagfield";
}
