package com.bounteous.core.models.datatypeproperties.impl;

import com.bounteous.core.models.datatypeproperties.CfxTagField;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.inject.Inject;

@Model(
    adaptables = {Resource.class, SlingHttpServletRequest.class},
    adapters = CfxTagField.class,
    resourceType = {CfxTagFieldImpl.RESOURCE_TYPE},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = "jackson", extensions = "json")
public class CfxTagFieldImpl extends AbstractCfxModelField implements CfxTagField {

    static final String RESOURCE_TYPE = "aem-cf-extras/cfm/models/datatypeproperties/cfx-tagfield";

    @Inject
    @ValueMapValue
    @Default(values = "")
    private String validation;

    @Inject
    @ValueMapValue
    @Default(values = "")
    private String rootPath;


    public String getSelectionCount() {
        return isMultiple() ? "multiple" : "single";
    }

    @Override
    public Object getValue() {
        final Object value = super.getValue();

        // TODO: Not sure this is necessary
        if (value instanceof String && StringUtils.isBlank((String)value)) {
            return new String[0];
        } else {
            return value;
        }
    }

    public String getValidation() {
        return validation;
    }

    public String getRootPath() {
        return StringUtils.defaultIfBlank(rootPath, "/content/cq:tags");
    }

}
