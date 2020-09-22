package com.bounteous.core.models.graphql.fieldmodels.cfx;

import com.bounteous.core.models.datatypeproperties.CfxParentField;
import com.bounteous.core.models.graphql.GraphQLBase;
import com.bounteous.core.models.graphql.fieldmodels.AbstractGraphQlDataTypeField;
import com.bounteous.core.models.graphql.fieldmodels.GraphQLContentFragmentField;
import com.bounteous.core.utils.ContentFragmentUtils;
import com.bounteous.core.utils.GraphQLUtils;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ElementTemplate;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import graphql.Scalars;
import graphql.execution.DataFetcherResult;
import graphql.language.FieldDefinition;
import graphql.language.ListType;
import graphql.language.TypeName;
import org.apache.commons.compress.utils.Lists;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.factory.ModelFactory;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Objects;

import static com.day.crx.JcrConstants.JCR_CONTENT;

@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        resourceType = {GraphQlCfxParentField.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GraphQlCfxParentField extends AbstractGraphQlDataTypeField {
    static final String RESOURCE_TYPE = "settings/dam/cfm/models/formbuilderconfig/datatypes/items/cfx-parent-field";

    private static final Logger LOG = LoggerFactory.getLogger(GraphQlCfxParentField.class);

    @Inject
    @Default(values = "")
    private String name;

    @Inject
    private boolean multiple;

    @Inject
    private String cfModel;

    @Inject
    private String cfModelPropertyName;

    @Inject
    @Self
    private CfxParentField cfxParentField;

    @Inject
    @Self
    private Resource resource;

    @Inject
    @Reference
    ModelFactory modelFactory;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isMultiValue() {
        return multiple;
    }

    public String getCfModel() {
        return cfModel;
    }

    @Override
    public String getGraphQLType() {
        Resource parentCfmResource = resource.getResourceResolver().getResource(getCfModel());
        if (parentCfmResource != null) {
            parentCfmResource = parentCfmResource.getChild(JCR_CONTENT);
            if (parentCfmResource != null) {
                FragmentTemplate parentCfm = parentCfmResource.adaptTo(FragmentTemplate.class);
                if (parentCfm != null) {
                    ElementTemplate parentField = Lists.newArrayList(parentCfm.getElements()).stream()
                            .filter(Objects::nonNull)
                            .filter(cfmElement -> cfmElement.getName().equals(cfModelPropertyName))
                            .findFirst()
                            .orElse(null);
                    if (parentField != null) {
                        GraphQLContentFragmentField parentDataTypeField = GraphQLUtils.getSlingModelClassForField(parentField, modelFactory);
                        if (parentDataTypeField != null) {
                            return parentDataTypeField.getGraphQLType();
                        }
                    }
                }
            }
        }

        return Scalars.GraphQLString.getName();
    }

    @Override
    public FieldDefinition getFieldDefinition() {
        LOG.debug(cfxParentField.toString() + ":" + resource.toString());
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
    protected DataFetcherResult getDataFetcherResult(final ContentElement contentElement, GraphQLBase.DataFetcherContext context) {
        ElementTemplate elementTemplate = ContentFragmentUtils.getElementTemplate(contentElement);
        Resource elementTemplateResource = elementTemplate.adaptTo(Resource.class);
        if (elementTemplateResource != null) {
            CfxParentField cfxParentField =
                elementTemplateResource.adaptTo(CfxParentField.class);
            if (cfxParentField != null) {
                Resource cfFieldResource = contentElement.adaptTo(Resource.class);
                if (cfFieldResource != null) {
                    return DataFetcherResult.newResult()
                        .data(cfxParentField.getValue())
                        .build();
                }
            }
        }

        return DataFetcherResult.newResult()
            .data(null)
            .build();
    }
}
