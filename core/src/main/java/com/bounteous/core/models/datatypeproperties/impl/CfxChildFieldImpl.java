package com.bounteous.core.models.datatypeproperties.impl;

import com.bounteous.core.models.datatypeproperties.CfxChildField;

import com.adobe.cq.dam.cfm.ContentFragment;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.bounteous.core.utils.ContentFragmentUtils.isFolder;
import static com.day.cq.commons.jcr.JcrConstants.JCR_CONTENT;

@Model(
    adaptables = {Resource.class, SlingHttpServletRequest.class},
    adapters = CfxChildField.class,
    resourceType = {CfxChildFieldImpl.RESOURCE_TYPE},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = "jackson", extensions = "json")
public class CfxChildFieldImpl extends AbstractCfxModelRelationField implements CfxChildField {
    private static final Logger LOGGER = LoggerFactory.getLogger(CfxChildFieldImpl.class);

    static final String RESOURCE_TYPE = "aem-cf-extras/cfm/models/datatypeproperties/cfx-child-field";

    private List<ContentFragment> childFragments;

    @Inject
    @ValueMapValue
    @Default(values = "")
    private String searchSubFolder;


    @PostConstruct
    public void init() {
        final String contentFragmentPath = getContainingContentFragmentPath();
        if (childFragments == null && contentFragmentPath != null) {
            childFragments = getValueList(contentFragmentPath);
        }
    }

    @Override
    public String getStringValue() {
        return CollectionUtils.isNotEmpty(childFragments) ?
            childFragments.stream().map(ContentFragment::getTitle).collect(Collectors.joining("\n")) :
            StringUtils.EMPTY;
    }

    @Override
    public Object getValue() {
        return childFragments;
    }

    //TODO: This method only remains public for GraphQlCFxChildField.getDataFetcherResult().  It should be made private once adaptation from
    // ContentElement is in place.
    @Override
    public List<ContentFragment> getValueList(final String contentFragmentPath) {
        final List<ContentFragment> valueList = new ArrayList<>();

        Resource contentFragmentResource = getResolver().getResource(contentFragmentPath);

        if (contentFragmentResource != null) {
            ContentFragment cf = contentFragmentResource.adaptTo(ContentFragment.class);
            if (cf == null && !isFolder(contentFragmentResource)) {
                return null;
            }

            valueList.addAll(getChildFragments(contentFragmentResource));
        }
        return valueList;
    }

    public List<ContentFragment> getChildFragments(final Resource resource) {
        if (resource != null) {
            List<ContentFragment> childList = new ArrayList<>();
            if (isFolder(resource)) {
                searchChildren(resource, childList, null);
            } else {
                searchChildren(resource.getParent(), childList, resource);
            }
            childList.sort((fragment1, fragment2) -> {
                String title1 = fragment1.getTitle();
                String title2 = fragment2.getTitle();
                title1 = title1 == null ? "" : title1.toUpperCase();
                title2 = title2 == null ? "" : title2.toUpperCase();
                return title1.compareTo(title2);
            });

            return childList;
        } else {
            return new ArrayList<>();
        }
    }

    private void searchChildren(Resource resource, List<ContentFragment> childList, Resource skipResource) {
        if (resource == null) {
            return;
        }

        for (Resource childResource : resource.getChildren()) {
            if (resource != skipResource) {
                Resource dataResource = null;
                Resource jcrResource = childResource.getChild(JCR_CONTENT);
                if (jcrResource != null) {
                    dataResource = jcrResource.getChild(DATA);
                }
                if (dataResource != null) {
                    String modelClass = (String) dataResource.getValueMap().get(CQ_MODEL);
                    if (cfModel != null && cfModel.equals(modelClass)) {
                        ContentFragment cf = childResource.adaptTo(ContentFragment.class);
                        try {
                            childList.add(cf);
                        } catch (Exception e) {
                            LOGGER.error("Unable to deserialize fragment '" + resource.getName() + "'", e);
                        }
                    }
                }
                searchChildren(childResource, childList, skipResource);
            }
        }
    }

    public String getSearchSubFolder() {
        return searchSubFolder;
    }

    @Override
    public String getRootPath() {
        return StringUtils.isNotBlank(searchSubFolder) ? getParentFolderPath() + "/" + searchSubFolder : null;
    }

    public String getParentFolderPath() {
        String parentPath = getRequest().getRequestPathInfo().getSuffix();
        return parentPath != null ? parentPath.substring(0, parentPath.lastIndexOf('/')) : null;
    }
}
