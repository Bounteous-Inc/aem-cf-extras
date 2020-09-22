package com.bounteous.core.cfm.admin.components.authoring.elementeditor;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.adobe.cq.dam.cfm.DataType;
import com.adobe.cq.dam.cfm.ElementTemplate;
import com.adobe.cq.dam.cfm.FragmentData;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.adobe.cq.dam.cfm.VariationTemplate;
import com.adobe.granite.ui.components.FormData;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.crx.JcrConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Much of this code is copied over from
 * `/libs/dam/cfm/admin/components/authoring/elementeditor/elementeditor.jsp`
 * in order to implement tabbing on content fragments.
 */
@Model(
        adaptables = SlingHttpServletRequest.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class ElementEditor {
    // TODO should be moved to an API

    private static final String RT_MULTIEDITOR = "dam/cfm/admin/components/authoring/contenteditor/multieditor";
    private static final String TAB_FIELD_TYPE = "cfx-tab-placeholder";
    private static final String PROP_REQUIRED = "required";

    @SlingObject
    private SlingHttpServletRequest slingRequest;

    @SlingObject
    protected ResourceResolver resourceResolver;

    private Map<String,List<Resource>> fragmentFieldsMap;

    private boolean hasTabs;

    private String variationToShow;

    @PostConstruct
    public void init() {
        fragmentFieldsMap = new LinkedHashMap<>();
        String contentPath = slingRequest.getRequestPathInfo().getSuffix();
        Resource cfRes = resourceResolver.getResource(contentPath);
        ContentFragment fragment = cfRes.adaptTo(ContentFragment.class);

        variationToShow = slingRequest.getParameter("variation");
        if ((variationToShow != null) && (variationToShow.length() == 0)) {
            variationToShow = null;
        }
        // resolve ":last" pseudo variation
        if (":last".equals(variationToShow)) {
            FragmentTemplate model = fragment.getTemplate();
            Iterator<VariationTemplate> variations = model.getVariations();
            VariationTemplate lastTemplate = null;
            while (variations.hasNext()) {
                lastTemplate = variations.next();
            }
            if (lastTemplate != null) {
                variationToShow = lastTemplate.getName();
            } else {
                variationToShow = null;
            }
        }

        ValueMap props = createDataValues(fragment, variationToShow);
        FormData.push(slingRequest, props, FormData.NameNotFoundMode.IGNORE_FRESHNESS);

        FragmentTemplate fragTpl = fragment.getTemplate();
        Iterator<ElementTemplate> elements = fragTpl.getElements();
        String currentTab = "General";
        while (elements.hasNext()) {
            ElementTemplate elementTpl = elements.next();

            Resource elementTemplateResource = elementTpl.adaptTo(Resource.class);
            if (elementTemplateResource != null) {
                ValueMap elementTemplateValueMap = elementTemplateResource.getValueMap();
                String metaType = elementTemplateValueMap.get("metaType", "");
                if (TAB_FIELD_TYPE.equals(metaType)) {
                    currentTab = elementTemplateValueMap.get("fieldLabel", "");
                    fragmentFieldsMap.put(currentTab, new ArrayList<>());
                    hasTabs = true;
                    continue;
                }
            }

            ContentElement element = fragment.getElement(elementTpl.getName());
            FragmentData data = getFragmentData(element, elementTpl, variationToShow);
            Resource renderRsc = determineRenderRsc(data, elementTpl, resourceResolver);
            if (variationToShow != null) {
                Resource graniteData = renderRsc.getChild("granite:data");
                Resource target = (graniteData != null ? graniteData : renderRsc);
                target.getValueMap().put("variation", variationToShow);
            }

            List<Resource> currentTabFields = fragmentFieldsMap.computeIfAbsent(currentTab, k -> new ArrayList<>());
            currentTabFields.add(renderRsc);
        }

        variationToShow = StringUtils.defaultString("");
    }

    public Map<String,List<Resource>> getFragmentFieldsMap() {
        return fragmentFieldsMap;
    }

    public boolean getHasTabs() {
        return hasTabs;
    }

    public String getVariationToShow() {
        return variationToShow;
    }

    public String popFormData() {
        FormData.pop(slingRequest);
        return "";
    }

    // --- Helpers -------------------------------------------------------------------------

    private void addDataAttribs(Map<String, Object> props, ElementTemplate tpl) {
        props.put("cfmInput", Boolean.TRUE.toString());
        props.put("element", tpl.getName());
        props.put("type", tpl.getDataType().getTypeString());
        props.put("type-multi", tpl.getDataType().isMultiValue());
    }

    private Resource createData(ElementTemplate tpl, ResourceResolver resolver,
                                Map<String, Object> addProps) {
        Map<String, Object> props = new HashMap<>();
        addDataAttribs(props, tpl);

        if (addProps != null) {
            for (String key : addProps.keySet()) {
                props.put(key, addProps.get(key));
            }
        }

        String path = tpl.adaptTo(Resource.class).getPath() + "/granite:data";
        return new ValueMapResource(
                resolver, path, JcrConstants.NT_UNSTRUCTURED, new ValueMapDecorator(props));
    }


    // --- Structured fragments ------------------------------------------------------------

    private void addOrMergeChild(List<Resource> otherChildren, Resource child) {
        boolean isMerged = false;
        for (Resource toCheck : otherChildren) {
            if (toCheck.getName().equals(child.getName())) {
                ValueMap existingProps = toCheck.getValueMap();
                ValueMap newProps = child.getValueMap();
                for (String name : newProps.keySet()) {
                    existingProps.put(name, newProps.get(name));
                }
                isMerged = true;
            }
        }
        if (!isMerged) {
            otherChildren.add(child);
        }
    }

    private Resource recreateVirtual(Resource base, ResourceResolver resolver,
                                     Map<String, Object> addProps, Resource... children) {
        Map<String, Object> props = new HashMap<String, Object>();
        ValueMap original = base.adaptTo(ValueMap.class);
        if (original != null) {
            Set<String> keys = original.keySet();
            for (String key : keys) {
                props.put(key, original.get(key));
            }
        }

        if (addProps != null) {
            for (String key : addProps.keySet()) {
                props.put(key, addProps.get(key));
            }
        }

        // the required property has to be handled differently
        if (props.containsKey(PROP_REQUIRED)) {
            String required = (String)props.get(PROP_REQUIRED);
            if (required != null) {
                if (required.equals("on")) {
                    props.put(PROP_REQUIRED, Boolean.TRUE.toString());
                } else {
                    props.put(PROP_REQUIRED, Boolean.FALSE.toString());
                }
            }
        }
        if (props.containsKey("renderReadOnly")) {
            props.put("renderReadOnly", "false");
        }

        String resourceType = null;
        if (props.containsKey("sling:resourceType")) {
            resourceType = (String) props.get("sling:resourceType");
        }

        List<Resource> virtualChildren = new ArrayList<>();
        Iterator<Resource> childResources = base.listChildren();
        while (childResources.hasNext()) {
            virtualChildren.add(recreateVirtual(childResources.next(), resolver, null));
        }
        if (children != null) {
            for (Resource child : children) {
                addOrMergeChild(virtualChildren, child);
            }
        }

        return new ValueMapResource(
                resolver, base.getPath(), resourceType, new ValueMapDecorator(props),
                virtualChildren);
    }

    private ValueMap createDataValues(ContentFragment fragment, String variationName) {
        Map<String, Object> props = new HashMap<String, Object>();
        FragmentTemplate template = fragment.getTemplate();
        Iterator<ElementTemplate> elements = template.getElements();
        while (elements.hasNext()) {
            ElementTemplate elementTpl = elements.next();
            ContentElement element = fragment.getElement(elementTpl.getName());
            FragmentData data = getFragmentData(element, elementTpl, variationName);
            props.put(elementTpl.getName(), (data != null ? data.getValue() : null));
        }
        return new ValueMapDecorator(props);
    }

    private Resource createStructured(FragmentData data, ElementTemplate tpl,
                                      ResourceResolver resolver) {
        Map<String, Object> addProps = new HashMap<String, Object>();
        if (data.getContentType() != null) {
            addProps.put("content-type", data.getContentType());
        }
        Resource tplRsc = recreateVirtual(
                tpl.adaptTo(Resource.class), resolver, null,
                createData(tpl, resolver, addProps));
        ValueMap props = tplRsc.getValueMap();
        // Hack for checkboxes, which work different
        if (tplRsc.isResourceType("granite/ui/components/coral/foundation/form/checkbox")) {
            Boolean val = data.getValue(Boolean.class);
            if ((val != null) && val) {
                // props.put("checked", true);
                props.put("value", "true");
            }
            props.put("uncheckedValue", "false");
        } else {
            props.put("value", data.getValue());
        }
        return tplRsc;
    }


    // --- Text-based fragments ------------------------------------------------------------

    private Resource createTextBased(FragmentData data, ElementTemplate tpl,
                                     ResourceResolver resolver) {
        String basePath = tpl.adaptTo(Resource.class).getPath();
        List<Resource> children = new ArrayList<Resource>(1);
        String content = data.getValue(String.class);
        String contentType = data.getContentType();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("name", tpl.getName());
        props.put("fieldLabel", tpl.getTitle());
        props.put("value", content);
        if ("text/html".equals(contentType)) {
            props.put("useFixedInlineToolbar", "true");
            props.put("renderReadOnly", "false");
        }

        Map<String, Object> addProps = new HashMap<String, Object>();
        addProps.put("content-type", contentType);
        children.add(createData(tpl, resolver, addProps));
        return new ValueMapResource(
                resolver, basePath,
                RT_MULTIEDITOR, new ValueMapDecorator(props), children);
    }

    // --- General -------------------------------------------------------------------------

    private FragmentData getFragmentData(ContentElement element, ElementTemplate tpl,
                                         String varName) {
        FragmentData data = null;
        if (element != null) {
            data = element.getValue();
            if (varName != null) {
                ContentVariation variation = element.getVariation(varName);
                if (variation != null) {
                    data = variation.getValue();
                }
            }
        } else {
            data = new NullData(tpl.getDataType(), tpl.getInitialContentType());
        }
        return data;
    }

    private boolean isStructured(ElementTemplate tpl) {
        Resource tplRsc = tpl.adaptTo(Resource.class);
        if (tplRsc == null) {
            return false;
        }
        return (tplRsc.getValueMap().containsKey("sling:resourceType"));
    }

    private Resource determineRenderRsc(FragmentData data, ElementTemplate tpl,
                                        ResourceResolver resolver) {
        if (isStructured(tpl)) {
            return createStructured(data, tpl, resolver);
        }
        return createTextBased(data, tpl, resolver);
    }


    // --- Inner class ---------------------------------------------------------------------

    static class NullData implements FragmentData {

        private final DataType dataType;

        private final String contentType;

        NullData(DataType dataType, String contentType) {
            this.dataType = dataType;
            this.contentType = contentType;
        }

        @Override
        public DataType getDataType() {
            return dataType;
        }

        @Override
        public <T> T getValue(Class<T> type) {
            return null;
        }

        @Override
        public Object getValue() {
            return null;
        }

        @Override
        public boolean isTypeSupported(Class type) {
            return true;
        }

        @Override
        public void setValue(Object value) {
            throw new UnsupportedOperationException("Setting values is unsupported.");
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public void setContentType(String contentType) {
            throw new UnsupportedOperationException("Setting content type is unsupported.");
        }
    }
}
