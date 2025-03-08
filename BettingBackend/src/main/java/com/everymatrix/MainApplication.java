package com.everymatrix;

import com.everymatrix.config.AppConfig;
import com.everymatrix.controller.MainController;
import com.everymatrix.server.CustomHttpServer;
import com.everymatrix.service.BetOfferService;
import com.everymatrix.service.SessionManager;

import java.io.IOException;

public class MainApplication {

    public static void main(String[] args) throws IOException {

        //object creation
        SessionManager sessionManager = new SessionManager(AppConfig.sessionTimeoutMilliseconds);
        BetOfferService betOfferService = new BetOfferService();
        MainController mainController = new MainController(sessionManager, betOfferService);



        //run the server
        CustomHttpServer server = new CustomHttpServer();
        server.registerRoutes(mainController);
        server.startServer(AppConfig.serverPort);
    }


}