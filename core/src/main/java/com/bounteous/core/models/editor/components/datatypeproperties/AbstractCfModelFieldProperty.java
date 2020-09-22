package com.bounteous.core.models.editor.components.datatypeproperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.inject.Inject;

public class AbstractCfModelFieldProperty {
    @Inject
    @SlingObject
    private Resource resource;

    @JsonIgnore
    @SlingObject
    private ResourceResolver resolver;

    @JsonIgnore
    @SlingObject
    private SlingHttpServletRequest request;


    public Resource getResource() {
        return resource;
    }

    public SlingHttpServletRequest getRequest() {
        return request;
    }

    public ResourceResolver getResolver() {
        return resolver;
    }

    public String getNameBase() {
        return "./content/items/" + resource.getName();
    }
}
