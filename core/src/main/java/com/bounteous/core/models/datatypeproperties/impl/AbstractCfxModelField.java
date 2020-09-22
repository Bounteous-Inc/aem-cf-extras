package com.bounteous.core.models.datatypeproperties.impl;

import com.bounteous.core.models.datatypeproperties.CfxModelField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.inject.Inject;

public abstract class AbstractCfxModelField implements CfxModelField {

    @Inject
    @SlingObject
    private Resource resource;

    @JsonIgnore
    @SlingObject
    private ResourceResolver resolver;

    @JsonIgnore
    @SlingObject
    private SlingHttpServletRequest request;

    @Inject
    @ValueMapValue()
    @Default(values = "")
    private String fieldLabel;

    @Inject
    @ValueMapValue
    @Default(values = "")
    private String fieldDescription;

    @Inject
    @ValueMapValue()
    @Default(values = "")
    private String name;

    @Inject
    @ValueMapValue
    @Default(values = "")
    private Object value;

    @Inject
    @ValueMapValue
    private String valueType;

    @Inject
    @ValueMapValue
    @Default(booleanValues = false)
    private boolean required;


    public String getFieldLabel() {
        return fieldLabel;
    }

    public String getFieldDescription() {
        return fieldDescription;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        //return fieldLabel == null ? text : fieldLabel;
        return fieldLabel == null ? "" :
            (required ? fieldLabel + " *" : fieldLabel);
    }

    public String getStringValue() {
        final Object valueObject = getValue();
        return valueObject == null ? "" : valueObject.toString();
    }

    public Object getValue() {
        return value;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isMultiple() {
        //TODO: Check the actual type on the CF value.  It could have changed in the model after the value was saved on the CF.
        return valueType.contains("[]");
    }

    /**
     * Get the content fragment path and convert it to a Resource.
     * @return a resource for the current content fragment
     */
    protected Resource getContainingContentFragmentResource() {
        return resolver.getResource(getContainingContentFragmentPath());
    }

    /**
     * When filling out a content fragment in AEM, the `resource` for the Sling Model will be the path to the model.
     * The actual content fragment path needs to be pulled from the suffix of the URL.
     * For example:
     * http://localhost:4502/editor.html/content/dam/aem-cf-extras/content-fragments/sports
     * where the path to the content fragment is
     * /content/dam/aem-cf-extras/content-fragments/sports
     * @return the path to the content fragment
     */
    protected String getContainingContentFragmentPath() {
        SlingHttpServletRequest request = this.request;

        if (request != null) {
            return request.getRequestPathInfo().getSuffix();
        } else {
            return null;
        }
    }

    protected ResourceResolver getResolver() {
        return resolver;
    }

    protected Resource getResource() {
        return resource;
    }

    protected SlingHttpServletRequest getRequest() {
        return request;
    }

    public String getNameBase() {
        return "./content/items/" + getResource().getName();
    }
}
