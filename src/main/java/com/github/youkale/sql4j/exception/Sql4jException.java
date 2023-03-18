package com.github.youkale.sql4j.exception;

public class Sql4jException extends RuntimeException{

    public Sql4jException() {
        super();
    }

    public Sql4jException(String message) {
        super(message);
    }

    public Sql4jException(String message, Throwable cause) {
        super(message, cause);
    }

    public Sql4jException(Throwable cause) {
        super(cause);
    }
}
