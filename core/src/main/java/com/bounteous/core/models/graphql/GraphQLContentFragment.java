package com.bounteous.core.models.graphql;

import com.bounteous.core.utils.GraphQLUtils;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.ElementTemplate;
import com.adobe.cq.dam.cfm.FragmentData;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.adobe.cq.dam.cfm.VariationDef;
import com.adobe.cq.dam.cfm.VariationTemplate;
import com.adobe.cq.dam.cfm.VersionDef;
import com.adobe.cq.dam.cfm.VersionedContent;
import com.drew.lang.annotations.NotNull;
import com.google.common.collect.Lists;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bounteous.core.models.graphql.GraphQLBase.ID_ATTRIBUTE;

/**
 * This class is a special implementation of the Adobe ContentFragment interface.
 * It uses a decorator pattern, keeping an instance of an adapted ContentFragment in the member variable `contentFragment`.
 * This allows it to take advantage of all the methods already implemented by Adobe but extend that by adding it's own.
 */
@Model(adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class GraphQLContentFragment implements ContentFragment {
    @Inject
    @Self
    private Resource resource;

    @Inject
    @Self
    private ContentFragment contentFragment;


    public boolean isValid() {
        return contentFragment != null;
    }

    public String getId() {
        return GraphQLUtils.getIdForResource(resource);
    }

    protected Map<String,Object> toMap() {
        Map<String,Object> cfMap = Lists.newArrayList(contentFragment.getElements()).stream()
                .filter(element -> element.getValue().getValue() != null)
                .collect(Collectors.toMap(ContentElement::getName, this::getFieldValue));

        cfMap.put(ID_ATTRIBUTE, getId());

        return cfMap;
    }

    public Object getFieldValue(@NotNull final ContentElement contentElement) {
        final FragmentData value = contentElement.getValue();
        if (value.getDataType().isMultiValue()) {
            return value.getValue();
        }

        return value.getValue(String.class);
    }

    public GraphQLContentFragmentModel getModel() {
        FragmentTemplate fragmentTemplate = getTemplate();
        if (fragmentTemplate != null) {
            Resource fragmentTemplateResource = fragmentTemplate.adaptTo(Resource.class);
            if (fragmentTemplateResource != null) {
                return fragmentTemplateResource.adaptTo(GraphQLContentFragmentModel.class);
            }
        }

        return null;
    }

    public ContentFragmentInfo getContentFragmentInfo() {
        GraphQLContentFragmentModel graphQLContentFragmentModel = getModel();
        return new ContentFragmentInfo(
            getId(),
            this.resource.getPath(),
            graphQLContentFragmentModel.getContentFragmentModelInfo()
        );
    }


    // Below are all methods that must be implemented from the ContentFragment interface

    @Override
    public String getName() {
        return contentFragment.getName();
    }

    @Override
    public ContentElement getElement(@NotNull final String elementName) {
        return contentFragment.getElement(elementName);
    }

    @Override
    public Iterator<ContentElement> getElements() {
        return contentFragment.getElements();
    }

    @Override
    public boolean hasElement(String s) {
        return contentFragment.hasElement(s);
    }

    @Override
    public ContentElement createElement(ElementTemplate elementTemplate) throws ContentFragmentException {
        return contentFragment.createElement(elementTemplate);
    }

    @Override
    public String getTitle() {
        return contentFragment.getTitle();
    }

    @Override
    public void setTitle(String s) throws ContentFragmentException {
        contentFragment.setTitle(s);
    }

    @Override
    public String getDescription() {
        return contentFragment.getDescription();
    }

    @Override
    public void setDescription(String s) throws ContentFragmentException {
        contentFragment.setDescription(s);
    }

    @Override
    public Map<String, Object> getMetaData() {
        return contentFragment.getMetaData();
    }

    @Override
    public void setMetaData(String s, Object o) throws ContentFragmentException {
        contentFragment.setMetaData(s, o);
    }

    @Override
    public Iterator<VariationDef> listAllVariations() {
        return contentFragment.listAllVariations();
    }

    @Override
    public FragmentTemplate getTemplate() {
        return contentFragment.getTemplate();
    }

    @Override
    public VariationTemplate createVariation(String s, String s1, String s2) throws ContentFragmentException {
        return contentFragment.createVariation(s, s1, s2);
    }

    @Override
    public void removeVariation(String s) throws ContentFragmentException {
        contentFragment.removeVariation(s);
    }

    @Override
    public Iterator<Resource> getAssociatedContent() {
        return contentFragment.getAssociatedContent();
    }

    @Override
    public void addAssociatedContent(Resource resource) throws ContentFragmentException {
        contentFragment.addAssociatedContent(resource);
    }

    @Override
    public void removeAssociatedContent(Resource resource) throws ContentFragmentException {
        contentFragment.removeAssociatedContent(resource);
    }

    @Override
    public VersionDef createVersion(String s, String s1) throws ContentFragmentException {
        return contentFragment.createVersion(s, s1);
    }

    @Override
    public Iterator<VersionDef> listVersions() throws ContentFragmentException {
        return contentFragment.listVersions();
    }

    @Override
    public VersionedContent getVersionedContent(VersionDef versionDef) throws ContentFragmentException {
        return contentFragment.getVersionedContent(versionDef);
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> adapterClass) {
        if (contentFragment == null) {
            return null;
        }
        return contentFragment.adaptTo(adapterClass);
    }

    /**
     * This class can be used to hold information about a Content Fragment without the need for an open resource resolver.
     */
    public static class ContentFragmentInfo {
        private String id;
        private String path;
        private GraphQLContentFragmentModel.ContentFragmentModelInfo contentFragmentModelInfo;

        public ContentFragmentInfo(String id, String path, GraphQLContentFragmentModel.ContentFragmentModelInfo contentFragmentModelInfo) {
            this.id = id;
            this.path = path;
            this.contentFragmentModelInfo = contentFragmentModelInfo;
        }

        public String getId() {
            return id;
        }

        public String getPath() {
            return path;
        }

        public GraphQLContentFragmentModel.ContentFragmentModelInfo getContentFragmentModelInfo() {
            return contentFragmentModelInfo;
        }
    }
}

