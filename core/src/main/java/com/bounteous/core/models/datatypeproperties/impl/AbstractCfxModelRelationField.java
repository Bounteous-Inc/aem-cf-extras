package com.bounteous.core.models.datatypeproperties.impl;

import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * A base class for any Content Fragment field that references another Content Fragment or multiple.
 */
public abstract class AbstractCfxModelRelationField extends AbstractCfxModelField {

    protected static final String CQ_MODEL = "cq:model";
    protected static final String MASTER = "master";
    protected static final String DATA = "data";

    @ValueMapValue
    @Default(booleanValues = false)
    private Boolean disabled;

    @ValueMapValue
    @Default(values = "")
    protected String cfModel;

    @ValueMapValue
    @Default(values = "")
    protected String rootPath;


    public Boolean getDisabled() {
        return disabled;
    }

    public String getRootPath() {
        return rootPath;
    }

}
