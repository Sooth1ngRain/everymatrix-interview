package com.everymatrix.controller;

import com.everymatrix.annotation.Route;
import com.everymatrix.model.HttpMethod;
import com.everymatrix.model.Session;
import com.everymatrix.model.StakeEntry;
import com.everymatrix.service.BetOfferService;
import com.everymatrix.service.SessionManager;
import com.everymatrix.utils.IOUtils;
import com.everymatrix.utils.UrlUtils;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MainController {

    private final SessionManager sessionManager;

    private final BetOfferService betOfferService;

    public MainController(SessionManager sessionManager, BetOfferService betOfferService) {
        this.sessionManager = sessionManager;
        this.betOfferService = betOfferService;
    }

    @Route(path = "/{customerId}/session")
    public String getSession(HttpExchange exchange, Map<String, String> pathVariables) throws IOException {
        String customerIdString = pathVariables.get("customerId");
        Integer customerId;
        try {
            customerId = Integer.parseInt(customerIdString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid customer ID format: " + customerIdString, e);
        }
        return sessionManager.getSession(customerId);
    }

    @Route(method = HttpMethod.POST, path = "/{betOfferId}/stake")
    public void placeStake(HttpExchange exchange, Map<String, String> pathVariables) throws IOException {
        String betOfferIdString = pathVariables.get("betOfferId");
        Integer betOfferId;
        try {
            betOfferId = Integer.parseInt(betOfferIdString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid bet offer ID format: " + betOfferIdString, e);
        }

        Map<String, String> urlParams = UrlUtils.getQueryParams(exchange.getRequestURI());
        String sessionKey = urlParams.get("sessionkey");

        String requestBody = IOUtils.readAllBytes(exchange.getRequestBody());
        Integer stake;
        try {
            stake = Integer.parseInt(requestBody.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid stake format: " + requestBody, e);
        }

        Session session = sessionManager.accessSession(sessionKey);
        betOfferService.placeStake(betOfferId, session.getCustomerId(), stake);
    }

    @Route(path = "/{betOfferId}/highstakes")
    public String queryHighStakes(HttpExchange exchange, Map<String, String> pathVariables) {
        String betOfferIdString = pathVariables.get("betOfferId");
        Integer betOfferId;
        try {
            betOfferId = Integer.parseInt(betOfferIdString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid bet offer ID format: " + betOfferIdString, e);
        }

        List<StakeEntry> stakeEntries = betOfferService.queryStakes(betOfferId);
        return StakeEntry.convertToCSV(stakeEntries);
    }
}