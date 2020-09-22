package com.bounteous.core.models.editor.components.datatypeproperties;

import com.adobe.granite.ui.components.formbuilder.FormResourceManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;

@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        resourceType = {CfxMultipleCheckboxField.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = "jackson", extensions = "json")
public class CfxMultipleCheckboxField extends AbstractCfModelFieldProperty {

    static final String RESOURCE_TYPE = "dam/cfm/models/editor/components/datatypeproperties/cfxmultiplecheckboxfield";

    @Inject
    private FormResourceManager formResourceManager;

    @Inject
    @ValueMapValue
    @Default(values = "false")
    private String multiple;

    private Resource placeholderFieldResource;

    @PostConstruct
    public void init() {
        HashMap<String, Object> values = new HashMap<>();
        values.put("granite:class",     "checkbox-label");
        values.put("text",              "Multiple Select");
        values.put("checked",           multiple);
        values.put("value",             "true");
        values.put("uncheckedValue",    "false");
        values.put("name",              getNameBase() + "/multiple");

        placeholderFieldResource = formResourceManager.getDefaultPropertyFieldResource(getResource(), values);
    }

    public Resource getPlaceholderFieldResource() {
        return placeholderFieldResource;
    }
}
