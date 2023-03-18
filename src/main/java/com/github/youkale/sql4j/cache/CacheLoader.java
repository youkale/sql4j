package com.github.youkale.sql4j.cache;

public interface CacheLoader<K,V> {

    V load(K k);
}
