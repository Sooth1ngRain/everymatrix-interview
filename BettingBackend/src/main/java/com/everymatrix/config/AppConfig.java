package com.everymatrix.config;

import com.everymatrix.utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

public class AppConfig {

    private static Logger log = LogUtils.getLogger();

    // Server request acquire thread timeout in milliseconds
    public static int serverMaxReqTime;

    // Server response timeout in milliseconds
    public static int serverMaxRspTime;

    // Server thread pool core pool size
    public static int serverThreadPoolCorePoolSize;

    // Server thread pool max pool size
    public static int serverThreadPoolMaxPoolSize;

    // Server thread pool keep-alive time in seconds
    public static int serverThreadPoolKeepAliveSeconds;

    // Number of high-stake bets returned per offer
    public static int highStakesSizeForBetOffer;

    // Session timeout in seconds
    public static int sessionTimeoutMilliseconds;

    // Server port
    public static int serverPort;

    private static final String EXTERNAL_CONFIG_FILE = "appconfig.properties";

    static {
        loadProperties();
    }

    private static void loadProperties() {
        Properties properties = new Properties();
        boolean loadedFromExternal = false;

        try {
            // load properties from the external file
            try (InputStream externalInput = Files.newInputStream(Paths.get(EXTERNAL_CONFIG_FILE))) {
                log.info("Loading configuration from external file: " + EXTERNAL_CONFIG_FILE);
                properties.load(externalInput);
                loadedFromExternal = true;
            }
        } catch (IOException e) {
            log.info("External configuration file not found. Falling back to internal resource.");
        }

        // If not loaded, fallback to internal properties
        if (!loadedFromExternal) {
            try (InputStream internalInput = AppConfig.class.getClassLoader().getResourceAsStream(EXTERNAL_CONFIG_FILE)) {
                if (internalInput == null) {
                    throw new RuntimeException("Unable to find internal appconfig.properties");
                }
                log.info("Loading configuration from internal resource: " + EXTERNAL_CONFIG_FILE);
                properties.load(internalInput);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load configuration from internal resource", e);
            }
        }

        try {
            serverMaxReqTime = Integer.parseInt(properties.getProperty("server.request.max-time", "-1"));
            System.setProperty("sun.net.httpserver.maxReqTime", String.valueOf(serverMaxReqTime));

            serverMaxRspTime = Integer.parseInt(properties.getProperty("server.response.max-time", "-1"));
            System.setProperty("sun.net.httpserver.maxRspTime", String.valueOf(serverMaxRspTime));

            serverThreadPoolCorePoolSize = Integer.parseInt(properties.getProperty("server.thread-pool.core-pool-size", "10"));
            serverThreadPoolMaxPoolSize = Integer.parseInt(properties.getProperty("server.thread-pool.max-pool-size", "100"));
            serverThreadPoolKeepAliveSeconds = Integer.parseInt(properties.getProperty("server.thread-pool.keep-alive-seconds", "60"));
            highStakesSizeForBetOffer = Integer.parseInt(properties.getProperty("bet-offer.topN-stakes.nums", "20"));
            sessionTimeoutMilliseconds = Integer.parseInt(properties.getProperty("session.timeout-milliseconds", "600000"));
            serverPort = Integer.parseInt(properties.getProperty("server.port", "8080"));

            log.info("Configuration successfully loaded.");
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid property value in configuration file", e);
        }
    }
}