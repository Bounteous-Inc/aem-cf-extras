package com.bounteous.core.common;

public class GraphQLServiceException extends RuntimeException {

    public GraphQLServiceException(String message) {
        super(message);
    }

    public GraphQLServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
