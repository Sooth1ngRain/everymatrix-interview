package com.everymatrix.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

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

    static {
        loadProperties();
    }

    private static void loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("appconfig.properties")) {
            if (input == null) {
                throw new RuntimeException("unable to find appconfig.properties");
            }
            properties.load(input);

            serverMaxReqTime = Integer.parseInt(properties.getProperty("server.request.max-time", "-1"));
            System.setProperty("sun.net.httpserver.maxReqTime" , String.valueOf(serverMaxReqTime));

            serverMaxRspTime = Integer.parseInt(properties.getProperty("server.response.max-time", "-1"));
            System.setProperty("sun.net.httpserver.maxRspTime" , String.valueOf(serverMaxRspTime));

            serverThreadPoolCorePoolSize = Integer.parseInt(properties.getProperty("server.thread-pool.core-pool-size", "10"));
            serverThreadPoolMaxPoolSize = Integer.parseInt(properties.getProperty("server.thread-pool.max-pool-size", "100"));
            serverThreadPoolKeepAliveSeconds = Integer.parseInt(properties.getProperty("server.thread-pool.keep-alive-seconds", "60"));
            highStakesSizeForBetOffer = Integer.parseInt(properties.getProperty("bet-offer.topN-stakes.nums", "20"));
            sessionTimeoutMilliseconds = Integer.parseInt(properties.getProperty("session.timeout-milliseconds", "600000"));
            serverPort = Integer.parseInt(properties.getProperty("server.port", "8080"));

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}