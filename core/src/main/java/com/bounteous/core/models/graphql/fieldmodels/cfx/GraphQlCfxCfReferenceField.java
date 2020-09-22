package com.bounteous.core.models.graphql.fieldmodels.cfx;

import com.bounteous.core.models.datatypeproperties.CfxCfReferenceField;
import com.bounteous.core.models.graphql.GraphQLBase;
import com.bounteous.core.models.graphql.GraphQLContentFragment;
import com.bounteous.core.utils.ContentFragmentUtils;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ElementTemplate;
import com.adobe.cq.dam.cfm.FragmentData;
import graphql.execution.DataFetcherResult;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bounteous.core.models.graphql.GraphQLBase.ID_ATTRIBUTE;
import static com.google.common.collect.Lists.newArrayList;

@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        resourceType = {GraphQlCfxCfReferenceField.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GraphQlCfxCfReferenceField extends GraphQlCFxChildField {
    static final String RESOURCE_TYPE = "settings/dam/cfm/models/formbuilderconfig/datatypes/items/cfx-cf-reference-field";

    @Inject
    @Default(values = "")
    private String name;

    @Inject
    private String valueType;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isMultiValue() {
        return valueType != null && valueType.contains("[]");
    }

    @Override
    protected DataFetcherResult getDataFetcherResult(final ContentElement contentElement, GraphQLBase.DataFetcherContext context) {
        ElementTemplate elementTemplate = ContentFragmentUtils.getElementTemplate(contentElement);
        Resource elementTemplateResource = elementTemplate.adaptTo(Resource.class);
        if (elementTemplateResource != null) {
            CfxCfReferenceField contentReferenceField = elementTemplateResource.adaptTo(CfxCfReferenceField.class);
            if (contentReferenceField != null) {
                Resource cfFieldResource = contentElement.adaptTo(Resource.class);
                if (cfFieldResource != null) {
                    GraphQLContentFragment graphQLContentFragment = cfFieldResource.adaptTo(GraphQLContentFragment.class);
                    if (graphQLContentFragment != null) {
                        ContentElement referenceContentElement = graphQLContentFragment.getElement(contentElement.getName());
                        if (referenceContentElement != null) {
                            ResourceResolver resolver = cfFieldResource.getResourceResolver();
                            String referenceContentElementValue = referenceContentElement.getContent();
                            FragmentData referenceFragmentData = referenceContentElement.getValue();
                            if (referenceFragmentData != null) {
                                String[] referenceContentElementValueArray = referenceFragmentData.getValue(String[].class);
                                if (referenceContentElementValueArray != null) {
                                    List<Map<String, Object>> returnList = new ArrayList<>();
                                    for (String referenceValue : referenceContentElementValueArray) {
                                        Map<String, Object> map = GraphQlCfxCfReferenceField.buildObjectMapUsingCFPath(resolver, referenceValue);
                                        if (map != null) {
                                            returnList.add(map);
                                        }
                                    }
                                    return DataFetcherResult.newResult()
                                        .data(returnList)
                                        .build();
                                } else {
                                    return DataFetcherResult.newResult()
                                        .data(buildObjectMapUsingCFPath(resolver, referenceContentElementValue))
                                        .build();
                                }
                            }
                        }
                    }
                }
            }
        }
        return DataFetcherResult.newResult()
            .data(null)
            .build();
    }

    private static Map<String, Object> buildObjectMapUsingCFPath(ResourceResolver resolver, String referenceContentElementValue) {
        Resource referenceResource = resolver.getResource(referenceContentElementValue);
        if (referenceResource != null) {
            GraphQLContentFragment graphQLReferenceFragment = referenceResource.adaptTo(GraphQLContentFragment.class);
            if (graphQLReferenceFragment != null && referenceResource.adaptTo(ContentFragment.class) != null) {
                Map<String, Object> fieldMap = newArrayList(
                        graphQLReferenceFragment.getElements()).stream()
                        .collect(Collectors.toMap(ContentElement::getName, ContentElement::getContent));
                fieldMap.put(ID_ATTRIBUTE, graphQLReferenceFragment.getId());
                return fieldMap;
            }
        }
        return null;
    }
}
