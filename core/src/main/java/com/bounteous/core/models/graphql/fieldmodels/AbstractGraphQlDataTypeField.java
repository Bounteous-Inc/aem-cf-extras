package com.bounteous.core.models.graphql.fieldmodels;

import com.bounteous.core.models.graphql.GraphQLBase;
import com.bounteous.core.utils.GraphQLUtils;

import com.adobe.cq.dam.cfm.ContentElement;
import graphql.Scalars;
import graphql.execution.DataFetcherResult;
import graphql.language.Field;
import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.ListType;
import graphql.language.TypeName;
import graphql.schema.DataFetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractGraphQlDataTypeField implements GraphQLContentFragmentField {

    @Override
    public String getGraphQLType() {
        return Scalars.GraphQLString.getName();
    }

    @Override
    public boolean getProducesValue() {
        return true;
    }

    @Override
    public List<InputValueDefinition> getInputValueDefinitions() {
        return new ArrayList<>();
    }

    @Override
    public DataFetcher getDataFetcher() {
        return dataFetchingEnvironment -> {
            final Field field = dataFetchingEnvironment.getField();
            Map<String,Object> sourceMap = dataFetchingEnvironment.getSource();
            Object cfField = sourceMap.get(field.getName());
            if (cfField instanceof ContentElement) {
                ContentElement contentElement = (ContentElement) cfField;
                GraphQLBase.DataFetcherContext context = dataFetchingEnvironment.getLocalContext();
                return getDataFetcherResult(contentElement, context);
            }

            return DataFetcherResult.newResult()
                    .data(cfField)
                    .build();
        };
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
    public boolean isFilterable() {
        return false;
    }

    protected DataFetcherResult getDataFetcherResult(final ContentElement contentElement, GraphQLBase.DataFetcherContext context) {
        return DataFetcherResult
            .newResult()
            .data(contentElement.getValue().getValue())
            .build();
    }

}
