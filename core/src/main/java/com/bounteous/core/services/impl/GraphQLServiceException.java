package com.bounteous.core.services.impl;

public class GraphQLServiceException extends RuntimeException {
    public GraphQLServiceException(Throwable cause) {
        super("An error occurred when trying to execute GraphQL query.", cause);
    }
}
