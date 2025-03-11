package com.everymatrix.service.unit;

import com.everymatrix.exception.SessionInvalidException;
import com.everymatrix.model.Session;
import com.everymatrix.service.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @Test
    public void testGetNewSession() {
        int customerId = 12345;
        String sessionKey = sessionManager.getNewSession(customerId);
        assertNotNull(sessionKey);
        Session session = sessionManager.accessSession(sessionKey);
        assertNotNull(session);
        assertEquals(customerId, session.getCustomerId());
    }

    @Test
    public void testAccessSessionBeforeExpiry() {
        int customerId = 12345;
        String sessionKey = sessionManager.getNewSession(customerId);
        assertDoesNotThrow(() -> sessionManager.accessSession(sessionKey));
    }

    @Test
    public void testAccessSessionAfterExpiry() throws InterruptedException {
        int customerId = 12345;
        String sessionKey = sessionManager.getNewSession(customerId);

        TimeUnit.MILLISECONDS.sleep(EXPIRATION_TIME + 100);

        assertThrows(SessionInvalidException.class, () -> sessionManager.accessSession(sessionKey));
    }

    @Test
    public void testPurgeExpiredSessions() throws InterruptedException {
        int customerId = 12345;
        String sessionKey = sessionManager.getNewSession(customerId);

        TimeUnit.MILLISECONDS.sleep(EXPIRATION_TIME + 100);

        sessionManager.purgeAllExpiredSession();
        assertThrows(SessionInvalidException.class, () -> sessionManager.accessSession(sessionKey));
    }

    @Test
    public void testSlidingExpiration() throws InterruptedException {
        int customerId = 12345;
        String sessionKey = sessionManager.getNewSession(customerId);

        for (int i = 0; i < 3; i++) {
            TimeUnit.MILLISECONDS.sleep(EXPIRATION_TIME / 2);
            assertDoesNotThrow(() -> sessionManager.accessSession(sessionKey));
        }

        TimeUnit.MILLISECONDS.sleep(EXPIRATION_TIME + 100);

        assertThrows(SessionInvalidException.class, () -> sessionManager.accessSession(sessionKey));
    }
}