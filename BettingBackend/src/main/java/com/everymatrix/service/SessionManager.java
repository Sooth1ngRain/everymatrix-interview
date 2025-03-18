package com.everymatrix.service;

import com.everymatrix.exception.SessionInvalidException;
import com.everymatrix.model.Session;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A lazy-expiration and periodically checking session manager.
 * Sessions support sliding refresh and session keys are generated using UUID.
 */
public class SessionManager {

    /**
     * key: sessionKey , value: session
     */
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final long expiredMilliseconds;
    private final ScheduledExecutorService executor;

    public SessionManager(long expiredMilliseconds) {
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.expiredMilliseconds = expiredMilliseconds;
        startPurgeTask();
    }

    /**
     * Retrieves or creates a session for a given customer ID.
     *
     * @param customerId the customer ID.
     * @return the session key associated with the customer ID.
     */
    public synchronized String getSession(Integer customerId) {
        validateCustomerId(customerId);
        return getOrCreateSession(customerId);
    }

    /**
     * Access the session based on the session key.
     *
     * @param sessionKey the session key.
     * @return the session object.
     * @throws SessionInvalidException if the session is invalid or expired.
     */
    public Session accessSession(String sessionKey) {
        validateSessionKey(sessionKey);

        Session session = sessions.get(sessionKey);
        if (session == null) {
            throw new SessionInvalidException();
        }
        if (isExpired(session)){
            removeExpiredSession(session);
            throw new SessionInvalidException();
        }
        refreshSession(session);
        return session;
    }

    private void removeExpiredSession(Session session) {
        if (session != null && isExpired(session)) {
            sessions.remove(session.getSessionKey());
        }
    }

    public void purgeAllExpiredSessions() {
        sessions.forEach((key, session) -> removeExpiredSession(session));
    }

    /**
     * Shuts down the periodic purge task.
     */
    public void shutdownPurgeTask() {
        executor.shutdown();
    }

    private String getOrCreateSession(int customerId) {

        Optional<Map.Entry<String, Session>> option = sessions.entrySet().parallelStream()
                .filter(entry -> entry.getValue().getCustomerId() == customerId && !isExpired(entry.getValue()))
                .findAny();
        if(option.isPresent()){
            Map.Entry<String, Session> sessionEntry = option.get();
            refreshSession(sessionEntry.getValue());
            return sessionEntry.getKey();
        } else {
            return createNewSession(customerId);
        }
    }

    private String createNewSession(int customerId) {
        String sessionKey = generateSessionKey(customerId);
        Session newSession = new Session(sessionKey, customerId);
        sessions.put(sessionKey, newSession);
        return sessionKey;
    }

    private void refreshSession(Session session) {
        session.setLatestAccessTime(System.currentTimeMillis());
    }

    private void startPurgeTask() {
        executor.scheduleAtFixedRate(this::purgeAllExpiredSessions, expiredMilliseconds, expiredMilliseconds, TimeUnit.MILLISECONDS);
    }

    private void validateCustomerId(Integer customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID should not be null.");
        }
    }

    private void validateSessionKey(String sessionKey) {
        if (sessionKey == null || sessionKey.isEmpty()) {
            throw new IllegalArgumentException("Session key should not be null or empty.");
        }


    }

    private boolean isExpired(Session session) {
        return (System.currentTimeMillis() - session.getLatestAccessTime()) > this.expiredMilliseconds;
    }

    private String generateSessionKey(int customerId) {
        return "C" + customerId + "T" + System.currentTimeMillis();
    }
}