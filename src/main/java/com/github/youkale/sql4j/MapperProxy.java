package com.github.youkale.sql4j;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import com.github.youkale.sql4j.annotation.Alias;
import com.github.youkale.sql4j.cache.CacheLoader;
import com.github.youkale.sql4j.cache.LRUCache;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Objects;

public class MapperProxy implements InvocationHandler {

    private final LRUCache<CacheKey, IFn> methodCache;

    private final Class<?> originInterface;

    private final DataSource dataSource;

    private static final IFn clojureRequire;

    private static final IFn createExecutor;

    private final String groupName;

    static {
        clojureRequire = Clojure.var("clojure.core", "require");
        clojureRequire.invoke(Clojure.read("sql4j.core"));
        createExecutor = Clojure.var("sql4j.core", "create-executor");
    }

    public MapperProxy(String groupName, Class<?> originInterface, DataSource dataSource) {
        this.groupName = groupName;
        this.originInterface = originInterface;
        this.dataSource = dataSource;
        this.methodCache = new LRUCache<>(30, new CacheLoader<>() {
            @Override
            public IFn load(CacheKey cacheKey) {
                return (IFn) createExecutor.invoke(cacheKey.getGroup(),
                        cacheKey.getMapperName(),
                        new MethodSignature(cacheKey.getMethod()));
            }
        });
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Alias alias = method.getAnnotation(Alias.class);
        String mapperName = originInterface.getName() + "#" + method.getName();
        if (null != alias) {
            mapperName = alias.value();
        }
        IFn executor = methodCache.get(new CacheKey(groupName, mapperName, method));
        if (null != executor) {
            try (Connection conn = this.dataSource.getConnection()) {
                return executor.invoke(conn, args);
            }
        } else {
            throw new BindingException("mapper [" + mapperName + "] is not binding");
        }
    }

    private static class CacheKey {
        private final String group;
        private final String mapperName;
        private final Method method;

        public CacheKey(String group, String mapperName, Method method) {
            this.group = Objects.requireNonNull(group);
            this.mapperName = Objects.requireNonNull(mapperName);
            this.method = Objects.requireNonNull(method);
        }

        public String getGroup() {
            return group;
        }

        public String getMapperName() {
            return mapperName;
        }

        public Method getMethod() {
            return method;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return group.equals(cacheKey.group) && mapperName.equals(cacheKey.mapperName) && method.equals(cacheKey.method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(group, mapperName, method);
        }
    }

}
