package com.bounteous.core.services;

import com.bounteous.core.models.graphql.GraphQLContentFragment;

import com.drew.lang.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface GraphQLService {
    Map<String, Object> executeQuery(@NotNull final String queryString);

    Map<String, Object> executeQueryForCfPath(@NotNull final String cfPath,
                                              @NotNull final List<String> attributes);

    String executeQueryAsJson(@NotNull final String queryString);

    GraphQLContentFragment.ContentFragmentInfo getGraphQLContentFragmentInfo(@NotNull final String cfPath);
}
