package com.bounteous.core.models.datatypeproperties.impl;

import com.bounteous.core.models.datatypeproperties.CfxTabPlaceholder;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
    adaptables = {Resource.class, SlingHttpServletRequest.class},
    adapters = CfxTabPlaceholder.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class CfxTabPlaceholderImpl implements CfxTabPlaceholder {

    @ValueMapValue
    @Default(values = "")
    private String fieldLabel;

    public String getFieldLabel() {
        return fieldLabel;
    }
}
