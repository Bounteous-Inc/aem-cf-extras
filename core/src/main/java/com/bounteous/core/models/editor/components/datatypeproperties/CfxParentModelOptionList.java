package com.bounteous.core.models.editor.components.datatypeproperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Model(
    adaptables = {Resource.class, SlingHttpServletRequest.class},
    resourceType = {CfxParentModelOptionList.RESOURCE_TYPE},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = "jackson", extensions = "json")
public class CfxParentModelOptionList extends AbstractCfModelFieldProperty {

    static final String RESOURCE_TYPE = "dam/cfm/models/editor/components/datatypeproperties/cfxparentmodleoptionlist";

    @Inject
    private String cfModel;

    private static final String CFM_MODEL_TEMPLATE = "/libs/settings/dam/cfm/model-types/fragment";

    @JsonIgnore
    private List<ParentModelOption> parentModelOptions;

    public List<ParentModelOption> getParentModelOptions() {
        if (parentModelOptions == null) {
            parentModelOptions = new ArrayList<>();
            Resource resource = getResolver().getResource(getRootPath());
            if (resource != null) {
                parentModelOptions = discoverModels(resource, parentModelOptions);
            }
        }
        return parentModelOptions;
    }

    private List<ParentModelOption> discoverModels(Resource resource, List<ParentModelOption> modelList) {
        for (Resource childResource : resource.getChildren()) {
            ValueMap valueMap = childResource.getValueMap();
            if (CFM_MODEL_TEMPLATE.equals(valueMap.get("cq:templateType"))) {
                ParentModelOption parentModelOption = new ParentModelOption();
                parentModelOption.name = (String) valueMap.get("jcr:title");
                parentModelOption.cfModel = childResource.getParent().getPath();
                modelList.add(parentModelOption);
                return modelList;
            }
            modelList = discoverModels(childResource, modelList);
        }
        return modelList;
    }

    public String getCfModel() {
        if (cfModel == null) {
            String requestPath = getRequest().getRequestPathInfo().getSuffix() + "/jcr:content/model/cq:dialog/" + getNameBase();
            Resource modelData = getResolver().getResource(requestPath);
            if (modelData != null) {
                cfModel = (String) modelData.getValueMap().get("cfModel");
            }
        }
        return cfModel;
    }

    public String getRootPath() {
        String requestPath = getRequest().getRequestPathInfo().getSuffix();
        if (StringUtils.isBlank(requestPath)) {
            return "/";
        }
        String[] requestPathParts = requestPath.split("/");
        if (requestPathParts.length <= 2) {
            return requestPath;
        }
        return "/" + requestPathParts[1] + "/"  + requestPathParts[2];
    }

    public static class ParentModelOption {
        public String name;
        public String cfModel;

        public String getName() {
            return name;
        }

        public String getCfModel() {
            return cfModel;
        }
    }

}
