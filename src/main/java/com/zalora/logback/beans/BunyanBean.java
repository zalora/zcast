package com.zalora.logback.beans;

import lombok.*;
import java.io.*;
import java.util.*;
import com.google.gson.Gson;
import java.text.SimpleDateFormat;
import ch.qos.logback.classic.spi.*;
import ch.qos.logback.classic.Level;
import java.lang.management.ManagementFactory;

/**
 * Represents a bunyan log event
 */
@ToString
@EqualsAndHashCode
public class BunyanBean {

    public static final Integer TRACE = 10;
    public static final Integer DEBUG = 20;
    public static final Integer INFO = 30;
    public static final Integer WARN = 40;
    public static final Integer ERROR = 50;
    public static final Integer FATAL = 60;

    public static final Integer SYSTEM_PID;
    public static final String SYSTEM_HOSTNAME;
    public static final Integer BUNYAN_VERSION = 0;
    public static final Map<Integer, Integer> levelMap;

    protected static final Gson gson;
    protected static final SimpleDateFormat BUNYAN_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    static {
        gson = new Gson();
        BUNYAN_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

        Map<Integer, Integer> lvlMap = new HashMap<>();
        lvlMap.put(Level.ALL_INTEGER, TRACE);
        lvlMap.put(Level.OFF_INTEGER, INFO); // Will there be formatting?
        lvlMap.put(Level.TRACE_INTEGER, TRACE);
        lvlMap.put(Level.DEBUG_INTEGER, DEBUG);
        lvlMap.put(Level.INFO_INTEGER, INFO);
        lvlMap.put(Level.WARN_INTEGER, WARN);
        lvlMap.put(Level.ERROR_INTEGER, ERROR);
        levelMap = Collections.unmodifiableMap(lvlMap);

        final Runtime rt = Runtime.getRuntime();
        Process p;
        String host;

        try {
            p = rt.exec("hostname");
            host = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
        } catch (Exception ex) {
            host = "";
        }

        SYSTEM_HOSTNAME = host;

        String pidHost = ManagementFactory.getRuntimeMXBean().getName();
        SYSTEM_PID = Integer.parseInt(pidHost.substring(0, pidHost.indexOf('@')));
    }

    @Getter @Setter
    private int level;

    @Getter @Setter
    private String name;

    @Getter @Setter
    private String msg;

    @Getter @Setter
    private String time;

    @Getter @Setter
    private String threadName;

    @Getter @Setter
    private String className;

    @Getter @Setter
    private StackTraceElement[] stackTrace;

    @Getter @Setter
    private IThrowableProxy exception;

    @Getter @Setter
    private Map<String, Object> context = new HashMap<>();

    @Getter
    private final int pid = SYSTEM_PID;

    @Getter
    private final String hostname = SYSTEM_HOSTNAME;

    @Getter
    private final int v = BUNYAN_VERSION;

    public BunyanBean(BunyanConfig config) {
        ILoggingEvent event = config.getEvent();

        Map<String, String> propMap = event.getMDCPropertyMap();
        Object[] args = event.getArgumentArray();

        if (propMap != null && propMap.size() > 0) {
            context.put("mdc", new HashMap<>(propMap));
        }

        if (args != null && args.length > 0) {
            context.put("args", Arrays.asList(args));
        }

        this.name = config.getAppName();
        this.className = event.getLoggerName();
        this.level = levelMap.get(event.getLevel().toInteger());

        // Make sure msg is not null, otherwise the key is dropped by logback
        this.msg = event.getFormattedMessage() != null ? event.getFormattedMessage() : "";
        this.threadName = event.getThreadName();
        this.time = BUNYAN_TIME_FORMAT.format(new Date(event.getTimeStamp()));
        this.className = event.getLoggerName();

        if (event.getLevel().toInt() >= config.getStackTraceLevel()) {
            this.stackTrace = event.getCallerData();
        }

        this.exception = event.getThrowableProxy();
    }

    public String toJson() {
        return gson.toJson(this);
    }
}
