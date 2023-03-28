package com.github.youkale.sql4j.cache;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * simple LRU cache
 * @param <K>
 * @param <V>
 */
public class LRUCache<K, V> {
    private final Map<K, V> cacheMap;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final int cacheSize;
    private final CacheLoader<K, V> loader;

    public LRUCache(int cacheSize, CacheLoader<K, V> loader) {
        this.cacheSize = cacheSize;
        this.loader = loader;
        cacheMap = new LinkedHashMap<>(cacheSize, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LRUCache.this.cacheSize;
            }
        };
    }

    public V get(K key) {
        V value;
        lock.readLock().lock();
        value = cacheMap.get(key);
        if (value != null) {
            lock.readLock().unlock();
            return value;
        }
        lock.readLock().unlock();
        lock.writeLock().lock();

        try {
            value = cacheMap.get(key);
            if (value == null) {
                value = loader.load(key);
                if (value != null) {
                    cacheMap.put(key, value);
                }
            }
        } catch (Exception e) {
            throw new CacheLoadingException(e);
        } finally {
            lock.writeLock().unlock();
        }

        return value;
    }

    public V put(K key, V value) {
        lock.writeLock().lock();
        try {
            return cacheMap.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            cacheMap.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}