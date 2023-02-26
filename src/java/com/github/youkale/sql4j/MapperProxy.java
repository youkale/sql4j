package com.github.youkale.sql4j;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import com.github.youkale.sql4j.annotation.Associate;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;

public class MapperProxy implements InvocationHandler {

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
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Associate associate = method.getAnnotation(Associate.class);
        String associateName = originInterface.getName() + "#" + method.getName();
        if (null != associate) {
            associateName = associate.value();
        }
        MethodSignature methodSignature = new MethodSignature(originInterface, method);
        IFn executor = (IFn) createExecutor.invoke(this.groupName, associateName, methodSignature);
        if (null != executor) {
            try (Connection conn = this.dataSource.getConnection()) {
                return executor.invoke(conn, args);
            }
        } else {
            throw new BindingException("mapper [" + associateName + "] is not binding");
        }
    }
}
