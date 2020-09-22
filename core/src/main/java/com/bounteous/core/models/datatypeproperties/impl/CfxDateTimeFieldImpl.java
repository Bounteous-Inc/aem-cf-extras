package com.bounteous.core.models.datatypeproperties.impl;

import com.bounteous.core.models.datatypeproperties.CfxDateTimeField;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.inject.Inject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Model(
    adaptables = {Resource.class, SlingHttpServletRequest.class},
    adapters = CfxDateTimeField.class,
    resourceType = {CfxDateTimeFieldImpl.RESOURCE_TYPE},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = "jackson", extensions = "json")
public class CfxDateTimeFieldImpl extends AbstractCfxModelField implements CfxDateTimeField {

    static final String RESOURCE_TYPE = "aem-cf-extras/cfm/models/datatypeproperties/cfx-datetime";

    @Inject
    @ValueMapValue
    @Default(values = "")
    private String validation;

    @Inject
    @ValueMapValue()
    @Default(values = "")
    private String text;

    @Inject
    @ValueMapValue()
    @Default(values = "")
    private String type;


    public Date getDate() {
        String dateString = getStringValue();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd[T]HH:mmZ");
        if (StringUtils.isBlank(dateString)) {
            return null;
        }
        try {
            return sdf.parse(dateString);
        } catch (ParseException pe) {
            throw new RuntimeException(pe);
        }
    }

    public String getValidation() {
        return validation;
    }

    public String getText() {
        return text;
    }

    public String getType() {
        return StringUtils.defaultIfBlank(type, "datetime");
    }

    public String getFormat() {
        if ("date".equals(type)) {
            return "YYYY-MM-DD";
        } else if ("time".equals(type)) {
            return "HH:mm";
        } else {
            return "YYYY-MM-DD[T]HH:mmZ";
        }
    }
}
