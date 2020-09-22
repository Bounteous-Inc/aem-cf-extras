package com.bounteous.core.models.datatypeproperties.impl;

import com.bounteous.core.models.datatypeproperties.CfxCfReferenceField;

import com.adobe.cq.dam.cfm.ContentFragment;
import com.day.cq.dam.api.DamConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Model(
    adaptables = {Resource.class, SlingHttpServletRequest.class},
    adapters = CfxCfReferenceField.class,
    resourceType = {CfxCfReferenceFieldImpl.RESOURCE_TYPE},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = "jackson", extensions = "json")
public class CfxCfReferenceFieldImpl extends AbstractCfxModelField implements CfxCfReferenceField {
    static final String RESOURCE_TYPE = "aem-cf-extras/cfm/models/datatypeproperties/cfx-cf-reference-field";

    @Inject
    @ValueMapValue
    @Default(values = DamConstants.MOUNTPOINT_ASSETS)
    private String rootPath;


    public String getRootPath() {
        return rootPath;
    }

    public ContentFragment getReferencedContentFragment() {
        if (getValue() != null) {
            Resource contentFragmentResource = getResolver().getResource(getStringValue());
            if (contentFragmentResource != null) {
                return contentFragmentResource.adaptTo(ContentFragment.class);
            }
        }
        return null;
    }

    public List<ContentFragment> getReferencedContentFragments() {
        if (getValue() == null) {
            return null;
        }
        if (getValue() instanceof String) {
            ContentFragment cf = getReferencedContentFragment();
            List<ContentFragment> returnList = new ArrayList<>();
            returnList.add(cf);
            return returnList;
        }
        List<ContentFragment> returnList = new ArrayList<>();
        for (String val: (String[]) getValue()) {
            Resource contentFragmentResource = getResolver().getResource(val);
            if (contentFragmentResource != null) {
                returnList.add(contentFragmentResource.adaptTo(ContentFragment.class));
            }
        }
        return returnList;
    }
}
