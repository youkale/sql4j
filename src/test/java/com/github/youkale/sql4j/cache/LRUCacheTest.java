package com.github.youkale.sql4j.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LRUCacheTest {

    private final LRUCache<String, Integer> cache = new LRUCache<>(2, key -> {
        if (key.equals("key3") || key.equals("key1")) {
            return null;
        } else {
            return Integer.valueOf(key.substring(3));
        }
    });

    @Test
    public void testGet() {
        cache.put("key1", 1);
        cache.put("key2", 2);

        Assertions.assertEquals(Integer.valueOf(1), cache.get("key1"));
        Assertions.assertEquals(Integer.valueOf(2), cache.get("key2"));
        Assertions.assertNull(cache.get("key3")); //缓存中不存在数据，需要从数据库中加载

        cache.put("key3", 3);
        Assertions.assertNull(cache.get("key1")); //缓存中的数据已经淘汰

        cache.get("key2");
        cache.put("key4", 4);
        Assertions.assertNull(cache.get("key3")); //缓存中的数据已经淘汰
    }

    @Test
    public void testPut() {
        Assertions.assertNull(cache.put("key1", 1));
        Assertions.assertNull(cache.put("key2", 2));

        Assertions.assertEquals(Integer.valueOf(1), cache.put("key1", 3));
        Assertions.assertEquals(Integer.valueOf(2), cache.put("key2", 4));

        Assertions.assertNull(cache.put("key3", 5)); //缓存中的数据已经淘汰
    }

    @Test
    public void testClear() {
        cache.put("key1", 1);
        cache.put("key2", 2);

        cache.clear();
        Assertions.assertNull(cache.get("key1"));
    }

}