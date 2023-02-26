package com.github.youkale.sql4j;


import com.github.youkale.sql4j.annotation.MapKey;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class MethodSignature {

    private final boolean returnsMany;
    private final boolean returnsMap;
    private final boolean returnsVoid;
    private final boolean returnsOptional;
    private final Class<?> returnType;
    private final String mapKey;

    private final Parameter[] parameters;

    public MethodSignature(Class<?> mapperInterface, Method method) {
        Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
        if (resolvedReturnType instanceof Class<?>) {
            this.returnType = (Class<?>) resolvedReturnType;
        } else if (resolvedReturnType instanceof ParameterizedType) {
            this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
        } else {
            this.returnType = method.getReturnType();
        }
        this.parameters = method.getParameters();
        this.returnsVoid = void.class.equals(this.returnType);
        this.returnsMany = returnType.isInstance(Collections.class) || this.returnType.isArray();
        this.returnsOptional = Optional.class.equals(this.returnType);
        this.mapKey = getMapKey(method);
        this.returnsMap = this.mapKey != null;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public boolean returnsMany() {
        return returnsMany;
    }

    public boolean returnsMap() {
        return returnsMap;
    }

    public boolean returnsVoid() {
        return returnsVoid;
    }

    public boolean returnsOptional() {
        return returnsOptional;
    }

    public String getMapKey() {
        return mapKey;
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    private String getMapKey(Method method) {
        String mapKey = null;
        if (Map.class.isAssignableFrom(method.getReturnType())) {
            final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
            if (mapKeyAnnotation != null) {
                mapKey = mapKeyAnnotation.value();
            }
        }
        return mapKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodSignature that = (MethodSignature) o;
        return returnsMany == that.returnsMany && returnsMap == that.returnsMap && returnsVoid == that.returnsVoid && returnsOptional == that.returnsOptional && Objects.equals(returnType, that.returnType) && Objects.equals(mapKey, that.mapKey) && Arrays.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(returnsMany, returnsMap, returnsVoid, returnsOptional, returnType, mapKey);
        result = 31 * result + Arrays.hashCode(parameters);
        return result;
    }
}
