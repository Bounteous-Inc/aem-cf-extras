package com.bounteous.core.models.editor.components.datatypeproperties;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;

@Model(
    adaptables = { Resource.class, SlingHttpServletRequest.class },
    resourceType = {CfxDateDataFormatPicker.RESOURCE_TYPE},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = "jackson", extensions = "json")
public class CfxDateDataFormatPicker {
    static final String RESOURCE_TYPE = "dam/cfm/models/editor/components/datatypeproperties/cfxdatedataformatpicker";
}
