package com.zalora.zcast;

import com.google.common.collect.Iterables;
import com.hazelcast.core.*;
import com.hazelcast.logging.*;
import com.google.common.base.Splitter;
import com.zalora.zcast.interceptor.CompressionInterceptor;

/**
 * Fire up HZ cluster
 */
public class Start {

    /**
     * Get list of maps to compress from cli
     * java -Dcompressed.maps=hz_memcache_map1,hz_memcache_map2
     * @param args Should be empty...
     */
    public static void main(String[] args) {
        Iterable<String> mapNames = Splitter.on(',')
            .trimResults()
            .omitEmptyStrings()
            .split(System.getProperty("compressed.maps", ""));

        final HazelcastInstance hz = Hazelcast.newHazelcastInstance();
        final LoggingService loggingService = hz.getLoggingService();
        final ILogger logger = loggingService.getLogger(Start.class.getClass());

        for (String mapName : mapNames) {
            hz.getMap(mapName).addInterceptor(new CompressionInterceptor(loggingService, mapName));
            logger.info(String.format("Added Compression Interceptor to %s", mapName));
        }

        if (Iterables.size(mapNames) == 0) {
            logger.severe("Launching ZCast without compression!");
        }
    }
}
