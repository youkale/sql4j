package com.github.youkale.sql4j.annotation;

import java.lang.annotation.*;

/**
 * query result mapping
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Results {
    Result[] value() default {};
}
