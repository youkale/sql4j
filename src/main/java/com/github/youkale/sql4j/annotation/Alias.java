package com.github.youkale.sql4j.annotation;

import java.lang.annotation.*;

/**
 * mapper alias
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Alias {

    String value();
}
