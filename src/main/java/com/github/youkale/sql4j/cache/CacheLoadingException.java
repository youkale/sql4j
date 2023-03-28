package com.github.youkale.sql4j.cache;

import com.github.youkale.sql4j.exception.Sql4jException;

/**
 * cache loading exception
 */
public class CacheLoadingException extends Sql4jException {
    public CacheLoadingException() {
    }

    public CacheLoadingException(String message) {
        super(message);
    }

    public CacheLoadingException(String message, Throwable cause) {
        super(message, cause);
    }

    public CacheLoadingException(Throwable cause) {
        super(cause);
    }
}
