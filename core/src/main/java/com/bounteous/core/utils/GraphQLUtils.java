package com.bounteous.core.utils;

import com.bounteous.core.models.graphql.fieldmodels.GraphQLContentFragmentField;

import com.adobe.cq.dam.cfm.ElementTemplate;
import com.drew.lang.annotations.NotNull;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.factory.ModelFactory;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class GraphQLUtils {
    private static final String CFM_FIELD_RESOURCE_TYPE_PREFIX = "settings/dam/cfm/models/formbuilderconfig/datatypes/items/";

    private GraphQLUtils() {
    }

    public static String getIdForResource(@NotNull final Resource resource) {
        return UUID.nameUUIDFromBytes(resource.getPath().getBytes()).toString();
    }

    public static String sanitizeForVariable(@NotNull final String name) {
        // Note that CaseFormat.LOWER_HYPHEN.to(LOWER_CAMEL, name) does not work because 'my-firstVar' becomes 'myFirstvar'
        final String sanitizedName = Arrays.stream(name.split("-"))
                .map(WordUtils::capitalize)
                .collect(Collectors.joining());

        return WordUtils.uncapitalize(sanitizedName).replaceAll("\\s","");
    }

    public static String sanitizeForType(@NotNull final String name) {
        return WordUtils.capitalize(sanitizeForVariable(name)).replaceAll("\\s","");
    }

    public static GraphQLContentFragmentField getSlingModelClassForField(@NotNull final ElementTemplate cfmField,
                                                                         @NotNull final ModelFactory modelFactory) {
        Resource cfmFieldResource = cfmField.adaptTo(Resource.class);
        ValueMap cfmFieldValueMap = cfmFieldResource.getValueMap();
        String metaType = cfmFieldValueMap.get("metaType", String.class);
        if (StringUtils.isNotBlank(metaType)) {
            Resource dataTypeResource = new SyntheticResource(cfmFieldResource.getResourceResolver(),
                    cfmFieldResource.getPath(), CFM_FIELD_RESOURCE_TYPE_PREFIX + metaType);
            //TODO: Find a way to mock this without using a static util method.
            Object slingModel = ContentFragmentUtils.getModelFromResource(modelFactory, dataTypeResource);

            //TODO: Find a way to check all Sling Models that match the metaType in case the first one does not extend GraphQLContentFragmentField
            if (slingModel instanceof GraphQLContentFragmentField) {
                return (GraphQLContentFragmentField) slingModel;
            }
        }

        return null;
    }
}
