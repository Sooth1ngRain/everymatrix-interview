package com.everymatrix.model;

import java.util.concurrent.atomic.AtomicBoolean;

public class Session {
    private final String sessionKey;
    private final int customerId;
    private final long expiredMilliseconds;
    private long latestAccessTime;

    private boolean deleted = false;

    public Session(String sessionKey, int customerId, long expiredMilliseconds) {
        this.sessionKey = sessionKey;
        this.customerId = customerId;
        this.expiredMilliseconds = expiredMilliseconds;
        this.latestAccessTime = System.currentTimeMillis();
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public int getCustomerId() {
        return customerId;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - this.latestAccessTime) > this.expiredMilliseconds;
    }

    public void setLatestAccessTime(long latestAccessTime) {
        this.latestAccessTime = latestAccessTime;
    }

    public long getLatestAccessTime() {
        return latestAccessTime;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }
}