package com.bounteous.core.models.datatypeproperties;

import com.adobe.cq.dam.cfm.ContentFragment;

import java.util.List;

public interface CfxChildField extends CfxModelField {
    List<ContentFragment> getValueList(final String contentFragmentPath);

    String getRootPath();
}
