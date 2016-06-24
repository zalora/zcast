package com.zalora.zcast;

import java.util.List;
import java.io.IOException;
import com.hazelcast.core.*;
import com.hazelcast.logging.*;
import org.yaml.snakeyaml.Yaml;
import com.zalora.zcast.interceptor.CompressionInterceptor;

/**
 * Fire up HZ cluster
 */
public class Start {

    /**
     * Add compression interceptor to memcached map
     * @param args CLI args are ignored
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        final Yaml yaml = new Yaml();
        final List<String> mapNames;

        final HazelcastInstance hz = Hazelcast.newHazelcastInstance();
        final LoggingService loggingService = hz.getLoggingService();
        final ILogger logger = loggingService.getLogger(Start.class.getClass());

        try {
            mapNames = (List<String>) yaml.load(Start.class.getClass().getResource("/compression.yml").openStream());
            for (String mapName : mapNames) {
                hz.getMap(mapName).addInterceptor(new CompressionInterceptor(loggingService, mapName));
                logger.info(String.format("Added Compression Interceptor to %s", mapName));
            }
        } catch (IOException ex) {
            logger.severe(ex);
        } catch (NullPointerException npe) {
            logger.severe("Couldn't process the compression config file, launching ZCast without compression!");
        }
    }
}
