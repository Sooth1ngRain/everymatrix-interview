package com.everymatrix.model;

import java.util.concurrent.atomic.AtomicBoolean;

public class Session {
    private final String sessionKey;
    private final int customerId;
    private long latestAccessTime;
    public Session(String sessionKey, int customerId) {
        this.sessionKey = sessionKey;
        this.customerId = customerId;
        this.latestAccessTime = System.currentTimeMillis();
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setLatestAccessTime(long latestAccessTime) {
        this.latestAccessTime = latestAccessTime;
    }

    public long getLatestAccessTime() {
        return latestAccessTime;
    }
}