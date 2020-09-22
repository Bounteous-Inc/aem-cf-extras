package com.bounteous.core.models.graphql.fieldmodels.cfx;

import com.bounteous.core.models.datatypeproperties.CfxChildField;
import com.bounteous.core.models.graphql.GraphQLBase;
import com.bounteous.core.models.graphql.GraphQLContentFragment;
import com.bounteous.core.models.graphql.fieldmodels.AbstractGraphQlDataTypeField;
import com.bounteous.core.utils.ContentFragmentUtils;
import com.bounteous.core.utils.GraphQLUtils;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ElementTemplate;
import com.google.common.collect.Lists;
import graphql.Scalars;
import graphql.execution.DataFetcherResult;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bounteous.core.models.graphql.GraphQLBase.ID_ATTRIBUTE;


@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        resourceType = {GraphQlCFxChildField.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GraphQlCFxChildField extends AbstractGraphQlDataTypeField {
    static final String RESOURCE_TYPE = "settings/dam/cfm/models/formbuilderconfig/datatypes/items/cfx-child-field";

    @Inject
    @Default(values = "")
    private String name;

    @Inject
    @Self
    private Resource resource;

    @Inject
    private String cfModel;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isMultiValue() {
        return true;
    }

    @Override
    protected DataFetcherResult getDataFetcherResult(final ContentElement contentElement, GraphQLBase.DataFetcherContext context) {
        ElementTemplate elementTemplate = ContentFragmentUtils.getElementTemplate(contentElement);
        Resource elementTemplateResource = elementTemplate.adaptTo(Resource.class);
        if (elementTemplateResource != null) {
            CfxChildField childField = elementTemplateResource.adaptTo(CfxChildField.class);
            if (childField != null) {
                Resource cfFieldResource = contentElement.adaptTo(Resource.class);
                if (cfFieldResource != null) {
                    List<ContentFragment> contentFragments =
                        childField.getValueList(StringUtils.defaultIfBlank(childField.getRootPath(), cfFieldResource.getPath()));

                    List<Map<String, Object>> resultList = contentFragments.stream()
                        /* TODO: Add filter for input attributes
                        .filter(cf -> args.keySet().size() == 0 || args.keySet().stream().allMatch(key -> {
                            if (key.equals(ID_ATTRIBUTE) && searchById) {
                                return cf.getId().equals(idArg);
                            }

                            FragmentData fieldValue = cf.getElement(key).getValue();
                            List argMatchList = (List) args.get(key);

                            return fieldValue != null &&
                                fieldValue.getValue(String.class) != null &&
                                argMatchList.contains(fieldValue.getValue(String.class));
                        }))
                         */
                        .map(cf -> cf.adaptTo(Resource.class))
                        .filter(Objects::nonNull)
                        .map(cfResource -> cfResource.adaptTo(GraphQLContentFragment.class))
                        .filter(Objects::nonNull)
                        .map(graphqlCf -> {
                            Map<String, Object> fieldMap = Lists.newArrayList(graphqlCf.getElements()).stream()
                                //.filter(cfElement -> cf.getFieldValue(cfElement) != null)
                                .collect(Collectors.toMap(ContentElement::getName, cfElement -> cfElement));

                            fieldMap.put(ID_ATTRIBUTE, graphqlCf.getId());

                            return fieldMap;
                        })
                        .collect(Collectors.toList());

                    return DataFetcherResult.newResult()
                        .data(resultList)
                        .build();
                }
            }
        }

        return DataFetcherResult.newResult()
            .data(null)
            .build();
    }

    @Override
    public String getGraphQLType() {
        Resource cfModelResource = resource.getResourceResolver().getResource(cfModel);
        if (cfModelResource != null) {
            return GraphQLUtils.sanitizeForType(cfModelResource.getName());
        }

        return Scalars.GraphQLString.getName();
    }

}
