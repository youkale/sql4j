package com.github.youkale.sql4j;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Keyword;
import com.github.youkale.sql4j.cache.LRUCache;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;

public class Sql4J {

    public final static String defaultMapperLocation = "sql";

    public final static String defaultMapperSuffix = ".sql";

    private final LRUCache<Class<?>, Object> proxyLRUCache;

    public static final String DEFAULT_GROUP_NAME = "sql4j";

    private static final IFn CLOJURE_REQUIRE;

    private static final IFn INIT_MAPPERS;


    static {
        CLOJURE_REQUIRE = Clojure.var("clojure.core", "require");
        CLOJURE_REQUIRE.invoke(Clojure.read("sql4j.core"));
        INIT_MAPPERS = Clojure.var("sql4j.core", "init-mappers");
    }

    public Sql4J(String groupName, DataSource dataSource, Dialect dialect, String mappersLocation, String mappersSuffix, boolean enableDebug) {
        INIT_MAPPERS.invoke(groupName, mappersLocation, mappersSuffix,
                "debug", enableDebug, "quoting", Keyword.intern(dialect.name()));
        this.proxyLRUCache = new LRUCache<>(100, clazz -> Proxy.newProxyInstance(clazz.getClassLoader(),
                new Class[]{clazz}, new MapperProxy(groupName, clazz, dataSource)));
    }

    public Sql4J(DataSource dataSource, Dialect dialect, String mappersLocation, boolean enableDebug) {
        this(DEFAULT_GROUP_NAME, dataSource, dialect, mappersLocation, defaultMapperSuffix, enableDebug);
    }

    public Sql4J(DataSource dataSource, Dialect dialect, String mappersLocation) {
        this(dataSource, dialect, mappersLocation, false);
    }

    public Sql4J(DataSource dataSource, Dialect dialect, boolean enableDebug) {
        this(dataSource, dialect, defaultMapperLocation, enableDebug);
    }

    public Sql4J(DataSource dataSource, Dialect dialect) {
        this(dataSource, dialect, defaultMapperLocation, false);
    }

    /**
     * lookup Mapper
     * @param clazz
     * @return
     * @param <T>
     */
    public <T> T lookup(Class<T> clazz) {
        return (T) proxyLRUCache.get(clazz);
    }

}
