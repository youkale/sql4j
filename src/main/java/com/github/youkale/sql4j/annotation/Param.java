package com.github.youkale.sql4j.annotation;

import java.lang.annotation.*;

/**
 * named parameter
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    String value();
}
