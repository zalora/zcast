package com.zalora.zcast;

import com.hazelcast.core.*;
import com.hazelcast.logging.*;
import com.hazelcast.config.Config;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
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

        final Config config = new Config()
            .setProperty("hazelcast.logging.type", "slf4j");

        final HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
        final LoggingService loggingService = hz.getLoggingService();
        final ILogger logger = loggingService.getLogger(Start.class.getClass());
        final CompressionInterceptor compressionInterceptor = new CompressionInterceptor();

        for (String mapName : mapNames) {
            hz.getMap(mapName).addInterceptor(compressionInterceptor);
            logger.info(String.format("Added Compression Interceptor to %s", mapName));
        }

        if (Iterables.size(mapNames) == 0) {
            logger.severe("Launching ZCast without compression!");
        }
    }
}
