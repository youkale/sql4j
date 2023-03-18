package com.github.youkale.sql4j;

import com.github.youkale.sql4j.exception.Sql4jException;

public class BindingException extends Sql4jException {
    public BindingException() {
    }

    public BindingException(String message) {
        super(message);
    }

    public BindingException(String message, Throwable cause) {
        super(message, cause);
    }

    public BindingException(Throwable cause) {
        super(cause);
    }
}
