package com.github.youkale.sql4j.annotation;

import java.lang.annotation.*;

/**
 * result mapping config
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Result {

    /**
     * java bean property
     *
     * @return
     */
    String property() default "";

    /**
     * database column
     *
     * @return
     */
    String column() default "";

}
