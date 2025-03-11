package com.everymatrix.service.unit;

import com.everymatrix.exception.SessionInvalidException;
import com.everymatrix.model.Session;
import com.everymatrix.service.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


public class SessionManagerTest {

    private static final long EXPIRATION_TIME = 1000;
    private SessionManager sessionManager;

    @BeforeEach
    public void setUp() {
        sessionManager = new SessionManager(EXPIRATION_TIME);
    }

    @AfterEach
    public void tearDown() {
        sessionManager.shutdownPurgeTask();
    }

    /**
     * 100 thread concurrently get new session
     * all thread should return the same session
     */
    @Test
    public void testGetNewSessionConcurrently() throws InterruptedException {
        final int customerId = 12345;
        final int threadCount = 100;
        final SessionManager sessionManager = new SessionManager(5000);

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        String[] sessionKeys = new String[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.execute(() -> {
                try {
                    startLatch.await(); // Wait for start signal
                    sessionKeys[index] = sessionManager.getSession(customerId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown(); // Signal completion
                }
            });
        }

        startLatch.countDown();
        endLatch.await();

        executorService.shutdown();

        String firstSessionKey = sessionKeys[0];
        boolean allSame = true;
        for (String sessionKey : sessionKeys) {
            if (!firstSessionKey.equals(sessionKey)) {
                allSame = false;
                break;
            }
        }
        assertTrue(allSame);
    }

    @Test
    public void testGetSessionInExpiryAndAfterExpiry() throws InterruptedException {
        int customerId = 12345;
        String sessionKey = sessionManager.getSession(customerId);
        assertNotNull(sessionKey);

        Thread.sleep(EXPIRATION_TIME / 2);
        String inExpirySessionKey = sessionManager.getSession(customerId);
        assertEquals(sessionKey, inExpirySessionKey);

        Thread.sleep((long)(EXPIRATION_TIME * 1.2));
        String afterExpirySessionKey = sessionManager.getSession(customerId);
        assertNotEquals(sessionKey, afterExpirySessionKey);
    }


    @Test
    public void testAccessSessionBeforeExpiry() {
        int customerId = 12345;
        String sessionKey = sessionManager.getSession(customerId);
        assertDoesNotThrow(() -> sessionManager.accessSession(sessionKey));
    }

    @Test
    public void testAccessSessionAfterExpiry() throws InterruptedException {
        int customerId = 12345;
        String sessionKey = sessionManager.getSession(customerId);

        TimeUnit.MILLISECONDS.sleep(EXPIRATION_TIME + 100);

        assertThrows(SessionInvalidException.class, () -> sessionManager.accessSession(sessionKey));
    }

    @Test
    public void testPurgeExpiredSessions() throws InterruptedException {
        int customerId = 12345;
        String sessionKey = sessionManager.getSession(customerId);

        TimeUnit.MILLISECONDS.sleep(EXPIRATION_TIME + 100);

        sessionManager.purgeAllExpiredSessions();
        assertThrows(SessionInvalidException.class, () -> sessionManager.accessSession(sessionKey));
    }

    @Test
    public void testSlidingExpiration() throws InterruptedException {
        int customerId = 12345;
        String sessionKey = sessionManager.getSession(customerId);
        Thread.sleep((long) (EXPIRATION_TIME * 0.8));
        String sameKey = sessionManager.getSession(customerId);
        Thread.sleep((long) (EXPIRATION_TIME * 0.8));
        assertEquals(sessionKey, sameKey);
        assertDoesNotThrow(() -> sessionManager.accessSession(sessionKey));
        Thread.sleep((long) (EXPIRATION_TIME * 0.8));
        assertDoesNotThrow(() -> sessionManager.accessSession(sessionKey));
        Thread.sleep((long) (EXPIRATION_TIME * 1.2));
        assertThrows(SessionInvalidException.class, () -> sessionManager.accessSession(sessionKey));

    }
}