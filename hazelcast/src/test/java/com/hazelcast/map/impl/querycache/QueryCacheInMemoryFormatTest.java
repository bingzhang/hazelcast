package com.hazelcast.map.impl.querycache;

import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.PredicateConfig;
import com.hazelcast.config.QueryCacheConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.QueryCache;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hazelcast.map.impl.querycache.AbstractQueryCacheTestSupport.getMap;
import static org.junit.Assert.assertEquals;

// keep serial runner, test operates on statistics
@RunWith(HazelcastSerialClassRunner.class)
@Category({QuickTest.class})
public class QueryCacheInMemoryFormatTest extends HazelcastTestSupport {

    @Test
    public void testObjectFormat_deserializeOneTime() {
        int expectedDeserializationCount = 1;
        testInMemoryFormat(InMemoryFormat.OBJECT, expectedDeserializationCount);
    }

    @Test
    public void testBinaryFormat_deserializeMoreTime() {
        int expectedDeserializationCount = 10;
        testInMemoryFormat(InMemoryFormat.BINARY, expectedDeserializationCount);
    }

    private void testInMemoryFormat(InMemoryFormat inMemoryFormat, int expectedDeserializationCount) {
        SerializableObject.deserializationCount.set(0);
        String mapName = randomString();
        String cacheName = randomString();

        Config config = new Config();
        MapConfig mapConfig = config.getMapConfig(mapName);

        QueryCacheConfig cacheConfig = new QueryCacheConfig(cacheName);
        cacheConfig.setInMemoryFormat(inMemoryFormat);
        PredicateConfig predicateConfig = cacheConfig.getPredicateConfig();
        predicateConfig.setSql("__key > -1");

        mapConfig.addQueryCacheConfig(cacheConfig);

        HazelcastInstance node = createHazelcastInstance(config);

        IMap<Integer, SerializableObject> map = getMap(node, mapName);

        map.put(1, new SerializableObject());

        QueryCache<Integer, SerializableObject> cache = map.getQueryCache(cacheName);

        for (int i = 0; i < 10; i++) {
            cache.get(1);
        }

        assertEquals(expectedDeserializationCount, SerializableObject.deserializationCount.get());
    }

    private static final class SerializableObject implements Serializable {

        private static final AtomicInteger deserializationCount = new AtomicInteger();

        private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
            deserializationCount.incrementAndGet();
        }
    }
}
