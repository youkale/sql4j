package com.github.youkale.sql4j;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Keyword;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;


public class Sql4J {
    public static final String DEFAULT_GROUP_NAME = "sql4j";

    private final DataSource dataSource;

    private static final IFn CLOJURE_REQUIRE;

    private static final IFn INIT_MAPPERS;

    private final String groupName;

    static {
        CLOJURE_REQUIRE = Clojure.var("clojure.core", "require");
        CLOJURE_REQUIRE.invoke(Clojure.read("sql4j.core"));
        INIT_MAPPERS = Clojure.var("sql4j.core", "init-mappers");
    }

    public Sql4J(String groupName, DataSource dataSource, Dialect dialect, String mappersLocation, String mappersSuffix, boolean enableDebug) {
        this.dataSource = dataSource;
        this.groupName = groupName;
        INIT_MAPPERS.invoke(groupName, mappersLocation, mappersSuffix,
                "debug", enableDebug, "quoting", Keyword.intern(dialect.name()));
    }

    public Sql4J(DataSource dataSource, Dialect dialect, String mappersLocation, boolean enableDebug) {
        this(DEFAULT_GROUP_NAME, dataSource, dialect, mappersLocation, ".sql", enableDebug);
    }

    public Sql4J(DataSource dataSource, Dialect dialect, String mappersLocation) {
        this(dataSource, dialect, mappersLocation, false);
    }

    public Sql4J(DataSource dataSource, Dialect dialect) {
        this(dataSource, dialect, "sql", false);
    }

    public <T> T lookup(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
                new Class[]{clazz}, new MapperProxy(groupName, clazz, dataSource));
    }

}
