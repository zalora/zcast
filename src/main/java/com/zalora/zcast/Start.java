package com.zalora.zcast;

import com.hazelcast.core.*;
import com.hazelcast.config.*;
import com.hazelcast.logging.*;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.zalora.zcast.interceptor.CompressionInterceptor;

/**
 * Fire up HZ cluster
 */
public class Start {

    private static final String BUNYAN_DEFAULT_LOG_PATH = "zcast.json";
    private static final CompressionInterceptor COMPRESSION_INTERCEPTOR = new CompressionInterceptor();

    /**
     * Get list of maps to compress from cli
     * java -Dcompressed.maps=hz_memcache_map1,hz_memcache_map2
     * @param args Should be empty...
     */
    public static void main(String[] args) {
        Config config = new XmlConfigBuilder().build();
        setupLogPath(config);

        final HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
        final LoggingService loggingService = hz.getLoggingService();
        final ILogger logger = loggingService.getLogger(Start.class.getClass());

        Iterable<String> mapNames = getCompressedMaps(config);
        for (String mapName : mapNames) {
            hz.getMap(mapName).addInterceptor(COMPRESSION_INTERCEPTOR);
            logger.info(String.format("Added Compression Interceptor to %s", mapName));
        }

        if (Iterables.size(mapNames) == 0) {
            logger.severe("Launching ZCast without compression!");
        }
    }

    private static Iterable<String> getCompressedMaps(Config config) {
        return Splitter.on(',')
            .trimResults()
            .omitEmptyStrings()
            .split(config.getProperty("zcast.maps"));
    }

    private static void setupLogPath(Config config) {
        String logPath = config.getProperty("zcast.logging.file");
        if (logPath == null) {
            logPath = BUNYAN_DEFAULT_LOG_PATH;
        }
        System.setProperty("zcast.logging.file", logPath);
    }
}
