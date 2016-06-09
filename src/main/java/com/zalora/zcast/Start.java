package com.zalora.zcast;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.zalora.zcast.interceptor.CompressionInterceptor;

/**
 * Fire up HZ cluster
 */
public class Start {

    public static final String ZCAST_DEFAULT_MAP = "hz_memcache_default";

    /**
     * Add compression interceptor to memcached map
     * @param args
     */
    public static void main(String[] args) {
        HazelcastInstance hz = Hazelcast.newHazelcastInstance();
        hz.getMap(ZCAST_DEFAULT_MAP).addInterceptor(new CompressionInterceptor(hz.getLoggingService()));

        hz.getLoggingService()
            .getLogger(Start.class.getName())
            .info(String.format("Added %s to map %s", CompressionInterceptor.class.getName(), ZCAST_DEFAULT_MAP));
    }
}
