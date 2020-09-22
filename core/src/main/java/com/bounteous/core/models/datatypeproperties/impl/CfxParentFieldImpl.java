package com.bounteous.core.models.datatypeproperties.impl;

import com.bounteous.core.models.datatypeproperties.CfxParentField;

import com.adobe.cq.dam.cfm.ElementTemplate;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import static com.day.cq.commons.jcr.JcrConstants.JCR_CONTENT;

@Model(
    adaptables = {ElementTemplate.class, Resource.class, SlingHttpServletRequest.class},
    adapters = CfxParentField.class,
    resourceType = {CfxParentFieldImpl.RESOURCE_TYPE},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = "jackson", extensions = "json")
public class CfxParentFieldImpl extends AbstractCfxModelRelationField implements CfxParentField {
    static final String RESOURCE_TYPE = "aem-cf-extras/cfm/models/datatypeproperties/cfx-parent-field";

    @Inject
    @ValueMapValue
    @Default(values = "")
    private String cfModelPropertyName;

    private Resource parentResource;

    @PostConstruct
    public void init() {
        loadParent(getContainingContentFragmentResource());
    }

    @Override
    public Object getValue() {
        ValueMap parentValueMap = parentResource.getValueMap();
        return parentResource != null
            ? parentValueMap.get(cfModelPropertyName)
            : null;
    }

    private void loadParent(final Resource contentFragmentResource) {
        if (parentResource == null && contentFragmentResource != null) {
            parentResource = getParentFragment(contentFragmentResource);
        }
    }

    private Resource getParentFragment(final Resource contentFragmentResource) {
        if (contentFragmentResource != null) {
            Resource parentResource = contentFragmentResource.getParent();
            if (parentResource == null) {
                return null;
            }
            return searchHierarchyForParent(parentResource);
        } else {
            return null;
        }
    }

    private Resource searchHierarchyForParent(final Resource parentResource) {
        for (Resource childResource : parentResource.getChildren()) {
            Resource dataResource = getDataResource(childResource);
            if (dataResource != null) {
                String modelClass = (String) dataResource.getValueMap().get(CQ_MODEL);

                if (StringUtils.isNotBlank(cfModel) && cfModel.equals(modelClass)) {
                    Resource masterResource = dataResource.getChild(MASTER);
                    if (masterResource != null) {
                        return masterResource;
                    }
                }
            }
        }

        final Resource grandparentResource = parentResource.getParent();
        if (grandparentResource != null) {
            return searchHierarchyForParent(grandparentResource);
        }

        return null;
    }

    private Resource getDataResource(final Resource childResource) {
        Resource dataResource = null;
        Resource jcrResource = childResource.getChild(JCR_CONTENT);
        if (jcrResource != null) {
            dataResource = jcrResource.getChild(DATA);
        }
        return dataResource;
    }

}
