package com.bounteous.core.models.graphql.fieldmodels;

import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.schema.DataFetcher;

import java.util.List;

public interface GraphQLContentFragmentField {

    String getName();

    boolean isMultiValue();

    DataFetcher getDataFetcher();

    String getGraphQLType();

    boolean getProducesValue();

    List<InputValueDefinition> getInputValueDefinitions();

    FieldDefinition getFieldDefinition();

    boolean isFilterable();
}
