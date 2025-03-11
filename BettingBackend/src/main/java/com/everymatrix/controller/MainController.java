package com.everymatrix.controller;

import com.everymatrix.annotation.PathVariable;
import com.everymatrix.annotation.RequestBody;
import com.everymatrix.annotation.RequestParam;
import com.everymatrix.annotation.Route;
import com.everymatrix.converter.IntegerParser;
import com.everymatrix.converter.LongParser;
import com.everymatrix.model.HttpMethod;
import com.everymatrix.model.Session;
import com.everymatrix.model.StakeEntry;
import com.everymatrix.service.BetOfferService;
import com.everymatrix.service.SessionManager;

import java.util.List;

public class MainController {

    private final SessionManager sessionManager;

    private final BetOfferService betOfferService;

    public MainController(SessionManager sessionManager, BetOfferService betOfferService) {

        this.sessionManager = sessionManager;
        this.betOfferService = betOfferService;
    }

    @Route(path = "/{customerId}/session")
    public String getSession(@PathVariable(value = "customerId", converter = IntegerParser.class) Integer customerId) {
        return sessionManager.getNewSession(customerId);
    }

    @Route(method = HttpMethod.POST, path = "/{betOfferId}/stake")
    public void placeStake(@RequestParam("sessionkey") String sessionKey,
                           @RequestBody(converter = IntegerParser.class) Integer stake,
                           @PathVariable(value = "betOfferId", converter = IntegerParser.class) Integer betOfferId) {
        Session session = sessionManager.accessSession(sessionKey);
        betOfferService.placeStake(betOfferId, session.getCustomerId(), stake);
    }

    @Route(path = "/{betOfferId}/highstakes")
    public String queryHighStakes(@PathVariable(value = "betOfferId", converter = IntegerParser.class) Integer betOfferId) {
        List<StakeEntry> stakeEntries = betOfferService.queryStakes(betOfferId);
        return StakeEntry.convertToCSV(stakeEntries);
    }
}
