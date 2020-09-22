package com.bounteous.core.utils;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ElementTemplate;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.day.cq.commons.jcr.JcrConstants;
import com.drew.lang.annotations.NotNull;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.models.factory.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class ContentFragmentUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ContentFragmentUtils.class);

    private ContentFragmentUtils() {

    }

    public static ElementTemplate getElementTemplate(@NotNull final ContentElement contentElement) {
        if (contentElement != null) {
            Resource cfFieldResource = (contentElement).adaptTo(Resource.class);
            if (cfFieldResource != null) {
                ContentFragment contentFragment = cfFieldResource.adaptTo(ContentFragment.class);
                if (contentFragment != null) {
                    FragmentTemplate cfm = contentFragment.getTemplate();
                    return cfm.getForElement((contentElement));
                }
            }
        }

        return null;
    }

    public static Date getLastModified(@NotNull final ContentElement contentElement) {
        if (contentElement != null) {
            final String elementName = contentElement.getName();
            final Resource cfResource = contentElement.adaptTo(Resource.class);
            if (cfResource != null) {
                Resource masterResource = cfResource.getChild("jcr:content/data/master");
                if (masterResource != null) {
                    final ValueMap masterValueMap = masterResource.getValueMap();
                    final String lastModifiedKey = elementName + "@LastModified";
                    if (masterValueMap.containsKey(lastModifiedKey)) {
                        return masterValueMap.get(lastModifiedKey, Date.class);
                    }
                }
            }
        }

        return null;
    }

    public static <T> T createModel(ModelFactory modelFactory, Resource resource, Class<T> klass) {
        if (resource == null) {
            LOG.warn("Create model requested for null object to class " + klass.getName());
            return null;
        }

        try {
            return modelFactory.createModel(resource, klass);
        } catch (Exception e) {
            LOG.error("Failed to create model for " + resource.getClass().getName() + " to " + klass.getName());
            return null;
        }
    }

    public static Object getModelFromResource(ModelFactory modelFactory, Resource dataTypeResource) {
        return modelFactory.getModelFromResource(dataTypeResource);
    }

    public static boolean isContentFragment(final String path, final ResourceResolver resourceResolver) {
        return isContentFragment(resourceResolver.getResource(path));
    }

    public static boolean isContentFragment(final Resource resource) {
        return resource != null && resource.adaptTo(ContentFragment.class) != null;
    }

    public static boolean isContentFragment(final SlingHttpServletRequest request) {
        return request != null && request.getRequestPathInfo().getSuffix() != null &&  !request.getRequestPathInfo().getSuffix().startsWith("/conf");
    }

    public static boolean isFolder(Resource resource) {
        return resource != null &&
            (resource.isResourceType(JcrConstants.NT_FOLDER) ||
                resource.getResourceType().equals(JcrResourceConstants.NT_SLING_FOLDER) ||
                resource.getResourceType().equals(JcrResourceConstants.NT_SLING_ORDERED_FOLDER));
    }

}
