package com.bounteous.core.models.datatypeproperties.impl;

import com.bounteous.core.models.datatypeproperties.CfxImageField;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
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
    adapters = CfxImageField.class,
    resourceType = {CfxImageFieldImpl.RESOURCE_TYPE},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = "jackson", extensions = "json")
public class CfxImageFieldImpl extends AbstractCfxModelField implements CfxImageField {

    static final String RESOURCE_TYPE = "aem-cf-extras/cfm/models/datatypeproperties/cfx-imagefield";

    @Inject
    @ValueMapValue
    @Default(booleanValues = false)
    private Boolean disabled;

    @Inject
    @ValueMapValue
    @Default(values = "")
    private String validation;


    public Boolean getDisabled() {
        return disabled;
    }

    public String getValidation() {
        return validation;
    }

    public String getThumbnail() {
        if (StringUtils.isBlank(getStringValue())) {
            return null;
        }
        Resource resource = getResolver().getResource(getStringValue());
        if (resource != null) {
            Asset asset = resource.adaptTo(Asset.class);
            if (asset != null) {
                Rendition rendition = asset.getRendition("cq5dam.thumbnail.140.100.png");
                if (rendition != null) {
                    return rendition.getPath();
                }
            }
        }
        return getStringValue();
    }

}
