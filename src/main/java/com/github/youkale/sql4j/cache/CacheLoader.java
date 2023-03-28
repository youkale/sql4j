package com.github.youkale.sql4j.cache;

/**
 * cache loader function
 * @param <K>
 * @param <V>
 */
public interface CacheLoader<K, V> {

    V load(K k) throws CacheLoadingException;
}
