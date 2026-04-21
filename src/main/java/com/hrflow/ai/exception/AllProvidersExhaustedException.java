package com.hrflow.ai.exception;

/**
 * Thrown when every provider in the fallback chain has failed
 * with a retriable error (rate limit, timeout, server error).
 */
public class AllProvidersExhaustedException extends RuntimeException {

    public AllProvidersExhaustedException(String message) {
        super(message);
    }

    public AllProvidersExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}
