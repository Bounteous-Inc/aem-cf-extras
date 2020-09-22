package com.bounteous.core.models.graphql.fieldmodels.adobe;

import com.bounteous.core.models.graphql.GraphQLBase;
import com.bounteous.core.models.graphql.fieldmodels.AbstractGraphQlDataTypeField;
import com.bounteous.core.utils.GraphQLUtils;

import com.adobe.cq.dam.cfm.ContentElement;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import graphql.execution.DataFetcherResult;
import graphql.language.FieldDefinition;
import graphql.language.ListType;
import graphql.language.TypeName;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        resourceType = {GraphQlTagsField.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GraphQlTagsField extends AbstractGraphQlDataTypeField {
    private static final Logger LOG = LoggerFactory.getLogger(GraphQlTagsField.class);

    static final String RESOURCE_TYPE = "settings/dam/cfm/models/formbuilderconfig/datatypes/items/tags";
    public static final String TAG_GRAPHQL_TYPE = "Tag";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";

    @Inject
    @Default(values = "")
    private String name;

    @Inject
    private String multiple;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isMultiValue() {
        return Boolean.parseBoolean(multiple);
    }

    @Override
    public FieldDefinition getFieldDefinition() {
        String graphQlType = getGraphQLType();
        return FieldDefinition.newFieldDefinition()
                .name(GraphQLUtils.sanitizeForVariable(getName()))
                .type(isMultiValue() ?
                        new ListType(new TypeName(graphQlType)) :
                        new TypeName(graphQlType))
                .inputValueDefinitions(getInputValueDefinitions())
                .build();
    }

    @Override
    public String getGraphQLType() {
        return TAG_GRAPHQL_TYPE;
    }

    private Map<String,Object> tagToMap(final Tag tag) {
        HashMap<String,Object> tagMap = new HashMap<>();
        if (tag != null) {
            Resource tagResource = tag.adaptTo(Resource.class);
            if (tagResource != null) {
                tagMap.put(ID, GraphQLUtils.getIdForResource(tagResource));
                tagMap.put(NAME, tag.getName());
                tagMap.put(TITLE, tag.getTitle());
                tagMap.put(DESCRIPTION, tag.getDescription());
            }
        }
        return tagMap;
    }

    @Override
    protected DataFetcherResult getDataFetcherResult(final ContentElement contentElement, GraphQLBase.DataFetcherContext context) {
        TagManager tagManager = context.getResourceResolver().adaptTo(TagManager.class);

        Object contentValue = contentElement.getValue().getValue();
        if (contentValue instanceof String[]) {
            List tagList = new ArrayList();
            for (String value: (String[])contentValue) {
                try {
                    Tag tag = tagManager.resolve(value);
                    tagList.add(tagToMap(tag));
                } catch (Exception e) {
                    LOG.error("Failed to resolve tag for field '" + contentElement.getName() + "', value = '" + value + "'", e);
                }
            }
            if ("false".equals(this.multiple)) {
                if (tagList.isEmpty() || ((Map)tagList.get(0)).isEmpty()) {
                    return DataFetcherResult.newResult().data(null).build();
                } else {
                    return DataFetcherResult.newResult().data(tagList.get(0)).build();
                }
            }
            return DataFetcherResult.newResult().data(tagList).build();
        } else {
            Tag tag = tagManager.resolve((String)contentValue);
            if (tag == null && "true".equals(this.multiple)) {
                return DataFetcherResult.newResult().data(new ArrayList<>()).build();
            }
            return DataFetcherResult.newResult().data(tagToMap(tag)).build();
        }
    }
}
