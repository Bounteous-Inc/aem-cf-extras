package com.bounteous.core.models.editor.components.datatypeproperties;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;

@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        resourceType = {CfxParentModelPropertyName.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = "jackson", extensions = "json")
public class CfxParentModelPropertyName extends AbstractCfModelFieldProperty {

    static final String RESOURCE_TYPE = "aem-cf-extras/cfm/models/editor/components/datatypeproperties/cfxparentmodelpropertyname";

    @Inject
    private String cfModelPropertyName;

    public String getCfModelPropertyName() {
        if (cfModelPropertyName == null) {
            String requestPath = getRequest().getRequestPathInfo().getSuffix() + "/jcr:content/model/cq:dialog/" + getNameBase();
            Resource modelData = getResolver().getResource(requestPath);
            if (modelData != null) {
                cfModelPropertyName = (String) modelData.getValueMap().get("cfModelPropertyName");
            }
        }
        return cfModelPropertyName;
    }

}
