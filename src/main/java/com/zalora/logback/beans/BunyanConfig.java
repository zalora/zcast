package com.zalora.logback.beans;

import lombok.*;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Configuration for Bunyan Layout
 */
@Data
public class BunyanConfig {

    /**
     * App name is needed to differ the log origin when everything is imported to Kibana
     */
    private final String appName;

    /**
     * logback log event object
     */
    private final ILoggingEvent event;

    /**
     * if the log level is equal or greater than this
     * @see ch.qos.logback.classic.Level
     */
    private final int stackTraceLevel;
}
