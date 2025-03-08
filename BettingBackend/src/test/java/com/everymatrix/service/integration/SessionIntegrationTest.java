package com.everymatrix.service.integration;

import com.everymatrix.config.AppConfig;
import com.everymatrix.controller.MainController;
import com.everymatrix.exception.HttpServerException;
import com.everymatrix.server.CustomHttpServer;
import com.everymatrix.service.BetOfferService;
import com.everymatrix.service.SessionManager;
import com.everymatrix.service.basic.BettingHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SessionIntegrationTest {

    public BettingHttpClient setUp() throws IOException {
        int port = new Random().nextInt(10000) + 30000;
        BettingHttpClient bettingHttpClient = new BettingHttpClient("http://localhost:" + port);
        SessionManager sessionManager = new SessionManager(1000);
        MainController mainController = new MainController(sessionManager, new BetOfferService());

        CustomHttpServer server = new CustomHttpServer();
        server.registerRoutes(mainController);
        server.startServer(port);
        return bettingHttpClient;
    }

    @Test
    public void testSessionBasic() throws IOException, InterruptedException {
        BettingHttpClient bettingHttpClient = setUp();
        Long customer1 = 1001L;
        Long betOfferId = 1L;
        String session1 = bettingHttpClient.getSession(customer1);
        assertNotNull(session1);
        Thread.sleep(500L);
        bettingHttpClient.postStake(betOfferId, session1, 200);
        Thread.sleep(2000L);

        try {
            bettingHttpClient.postStake(betOfferId, session1, 200);
            assert false;
        } catch (HttpServerException e){
            assert e.getHttpStatusCode() == 401;
        }
    }

    @Test
    public void testSessionSlidingRefresh() throws IOException, InterruptedException {
        BettingHttpClient bettingHttpClient = setUp();
        Long customer1 = 1001L;
        String session1 = bettingHttpClient.getSession(customer1);
        bettingHttpClient.postStake(1L, session1, 200);
        Thread.sleep(1100L);
        try {

            bettingHttpClient.postStake(1L, session1, 300);
            assert false;
        } catch (HttpServerException ex) {
            assert ex.getHttpStatusCode() == 401;
        } catch (Exception ex) {
            assert false;
        }
    }

    @Test
    public void testSessionIllegalArg() throws IOException, InterruptedException {
        try {
            BettingHttpClient bettingHttpClient = setUp();
            String session1 = bettingHttpClient.getSession(null);
            assert false;
        } catch (HttpServerException e) {
            assert e.getHttpStatusCode() == 400;
        }


    }


}
