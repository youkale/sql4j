package com.github.youkale.sql4j;


import com.github.youkale.sql4j.annotation.Result;
import com.github.youkale.sql4j.annotation.Results;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodSignature {

    private final Class<?> returnType;
    private final Class<?>[] genericReturnTypes;

    private final Map<String,String> resultMapping;

    private final Parameter[] parameters;

    public MethodSignature(Method method) {
        Type resolvedReturnType = method.getGenericReturnType();
        if (resolvedReturnType instanceof Class<?>) {
            this.returnType = (Class<?>) resolvedReturnType;
            this.genericReturnTypes = null;
        } else if (resolvedReturnType instanceof ParameterizedType) {
            this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
            this.genericReturnTypes = Stream.of(((ParameterizedType) resolvedReturnType).getActualTypeArguments())
                    .filter(c -> c instanceof Class).map(c -> (Class<?>) c).toArray(Class[]::new);
        } else {
            this.returnType = method.getReturnType();
            this.genericReturnTypes = null;
        }

        this.parameters = method.getParameters();
        this.resultMapping = getResultMap(method);
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public Class<?>[] getGenericReturnTypes() {
        return genericReturnTypes;
    }

    public Map<String, String> getResultMapping() {
        return resultMapping;
    }

    private Map<String, String> getResultMap(Method method) {
        Results results = method.getAnnotation(Results.class);
        if (null != results) {
            return Stream.of(results.value()).collect(Collectors.toMap(Result::column, Result::property));
        } else {
            return Collections.emptyMap();
        }
    }

}
