package com.everymatrix.service;

import com.everymatrix.exception.SessionExpiredException;
import com.everymatrix.exception.SessionInvalidException;
import com.everymatrix.model.Session;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A lazy-expiration and periodically checking session manager.
 * Session keys support sliding refresh, and are generated using UUID.
 */
public class SessionManager {

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final long expiredMilliseconds;
    private final ScheduledExecutorService executor;

    public SessionManager(long expiredMilliseconds) {
        this.expiredMilliseconds = expiredMilliseconds;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        startPurgeTask();
    }

    /**
     * Generates a new session key.
     *
     * @return A unique session key.
     */
    public String getNewSession(Integer customerId) {
        if(customerId == null){
            throw new IllegalArgumentException("customerId should not be null");
        }
        String sessionKey = UUID.randomUUID().toString();
        Session session = new Session(sessionKey, customerId , expiredMilliseconds);
        sessions.put(sessionKey, session);
        return sessionKey;
    }

    /**
     * Accesses the session key, sliding the refresh time upon each access.
     * Throws {@link SessionExpiredException} if the session key is expired.
     *
     */
    public Session accessSession(String sessionKey) {
        if(sessionKey == null || sessionKey.isEmpty()){
            throw new IllegalArgumentException("sessionKey should not be null");
        }

        Session session = sessions.get(sessionKey);
        if (session == null) {
            throw new SessionExpiredException();
        }
        if(session.isExpired()){
            session.setDeleted(true);
            throw new SessionInvalidException();
        }
        session.setLatestAccessTime(System.currentTimeMillis());
        return session;
    }

    /**
     * Purges all expired sessions by iterating over each session and checking its expiration status.
     */
    public void purgeAllExpiredSession() {
        sessions.forEach((key, session) -> {
            if(session.isDeleted()){
                sessions.remove(key);
            }
        });
    }

    /**
     * Starts a scheduled task to purge all expired sessions at fixed intervals.
     */
    public void startPurgeTask() {
        executor.scheduleAtFixedRate(this::purgeAllExpiredSession, expiredMilliseconds, expiredMilliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * Shuts down the scheduler service when no longer needed.
     */
    public void shutdownPurgeTask() {
        executor.shutdown();
    }
}