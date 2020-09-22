package com.bounteous.core.models.datatypeproperties.impl;

import com.bounteous.core.models.datatypeproperties.CfxIdField;
import com.bounteous.core.utils.GraphQLUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import javax.annotation.PostConstruct;

import static com.day.cq.commons.jcr.JcrConstants.JCR_PRIMARYTYPE;
import static com.day.cq.dam.api.DamConstants.NT_DAM_ASSET;

@Model(
    adaptables = {Resource.class, SlingHttpServletRequest.class},
    adapters = CfxIdField.class,
    resourceType = {CfxIdFieldImpl.RESOURCE_TYPE},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class CfxIdFieldImpl extends AbstractCfxModelField implements CfxIdField {

    static final String RESOURCE_TYPE = "aem-cf-extras/cfm/models/datatypeproperties/cfx-id-field";

    private Object value;

    @PostConstruct
    public void init() {
        //TODO: There should be a better way to get the CF path for this field
        this.value = StringUtils.EMPTY;
        final String resourcePath = getRequest().getRequestPathInfo().getSuffix();
        if (StringUtils.isNotBlank(resourcePath)) {
            final Resource cfResource = getResolver().getResource(resourcePath);
            if (cfResource != null && cfResource.getValueMap().get(JCR_PRIMARYTYPE).equals(NT_DAM_ASSET)) {
                this.value = GraphQLUtils.getIdForResource(cfResource);
            }
        }
    }

    @Override
    public Object getValue() {
        return value;
    }

}
