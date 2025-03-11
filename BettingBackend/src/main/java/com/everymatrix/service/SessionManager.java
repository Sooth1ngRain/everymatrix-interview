package com.everymatrix.service;

import com.everymatrix.exception.SessionInvalidException;
import com.everymatrix.model.Session;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A lazy-expiration and periodically checking session manager.
 * Session keys support sliding refresh, and are generated using UUID.
 */
public class SessionManager {

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> customIdIndex = new ConcurrentHashMap<>();
    private final long expiredMilliseconds;
    private final ScheduledExecutorService executor;


    private final ConcurrentHashMap<Integer, Lock> customerLocks = new ConcurrentHashMap<>(); // locks for each customerId

    public SessionManager(long expiredMilliseconds) {
        this.expiredMilliseconds = expiredMilliseconds;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        startPurgeTask();
    }

    public String getSession(Integer customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId should not be null");
        }

        Lock lock = customerLocks.computeIfAbsent(customerId, k -> new ReentrantLock());
        lock.lock();
        try {
            String sessionKey = customIdIndex.get(customerId);
            if (sessionKey != null) {
                Session session = sessions.get(sessionKey);
                if (session.isExpired()) {
                    sessions.remove(sessionKey);
                    customIdIndex.remove(session.getCustomerId());
                } else {
                    refreshSession(session);
                    return sessionKey;
                }

            }

            // Create new session if no valid session exists
            return createNewSession(customerId);
        } finally {
            lock.unlock(); // Release the lock
        }
    }

    private String createNewSession(int customerId) {
        String sessionKey = UUID.randomUUID().toString().replace("-", "");
        Session session = new Session(sessionKey, customerId, expiredMilliseconds);
        sessions.put(sessionKey, session);
        customIdIndex.put(customerId, sessionKey);
        return sessionKey;
    }

    public Session accessSession(String sessionKey) {
        if (sessionKey == null || sessionKey.isEmpty()) {
            throw new IllegalArgumentException("sessionKey should not be null or empty");
        }

        Session session = sessions.get(sessionKey);
        if (session == null || session.isExpired()) {
            removeExpiredSession(sessionKey);
            throw new SessionInvalidException();
        }

        refreshSession(session);
        return session;
    }

    public void removeExpiredSession(String sessionKey) {
        Session session = sessions.get(sessionKey);
        if (session != null && session.isExpired()) {
            sessions.remove(sessionKey);
            customIdIndex.remove(session.getCustomerId());
        }
    }

    public void purgeAllExpiredSessions() {
        sessions.forEach((key, session) -> removeExpiredSession(key));
    }

    private void startPurgeTask() {
        executor.scheduleAtFixedRate(this::purgeAllExpiredSessions, expiredMilliseconds, expiredMilliseconds, TimeUnit.MILLISECONDS);
    }

    public void shutdownPurgeTask() {
        executor.shutdown();
    }

    private void refreshSession(Session session) {
        session.setLatestAccessTime(System.currentTimeMillis());
    }
}