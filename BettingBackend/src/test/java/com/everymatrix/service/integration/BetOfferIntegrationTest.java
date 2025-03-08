package com.everymatrix.service.integration;

import com.everymatrix.config.AppConfig;
import com.everymatrix.controller.MainController;
import com.everymatrix.exception.HttpServerException;
import com.everymatrix.server.CustomHttpServer;
import com.everymatrix.service.BetOfferService;
import com.everymatrix.service.SessionManager;
import com.everymatrix.service.basic.BettingHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BetOfferIntegrationTest {

    public BettingHttpClient setUp() throws IOException {
        int port = new Random().nextInt(10000) + 30000;

        AppConfig.sessionTimeoutMilliseconds = 600000;
        AppConfig.highStakesSizeForBetOffer = 20;
        AppConfig.serverThreadPoolCorePoolSize = 100;
        AppConfig.serverThreadPoolMaxPoolSize = 200;

        BettingHttpClient bettingHttpClient = new BettingHttpClient("http://localhost:" + port);
        SessionManager sessionManager = new SessionManager(AppConfig.sessionTimeoutMilliseconds);
        MainController mainController = new MainController(sessionManager, new BetOfferService());

        //run the server
        CustomHttpServer server = new CustomHttpServer();
        server.registerRoutes(mainController);
        server.startServer(port);
        return bettingHttpClient;
    }

    @Test
    public void testPlaceStake() throws IOException {
        BettingHttpClient bettingHttpClient = setUp();
        Long customer1 = 1001L;
        Long customer2 = 1002L;
        Long betOffer1 = 9003L;

        String session1_0 = bettingHttpClient.getSession(customer1);
        String session1_1 = bettingHttpClient.getSession(customer1);

        String session2 = bettingHttpClient.getSession(customer2);

        //basic test
        bettingHttpClient.postStake(betOffer1, session1_0, 200);
        bettingHttpClient.postStake(betOffer1, session1_1, 300);
        bettingHttpClient.postStake(betOffer1, session2, 100);

        String highestStake = bettingHttpClient.getHighestStake(betOffer1);
        assert highestStake.equals(String.format("%d=%d,%d=%d", customer1, 300, customer2, 100));

    }

    @Test
    public void testPlaceStakeWithConcurrency() throws IOException {
        BettingHttpClient bettingHttpClient = setUp();

        final int totalThreads = 100;
        final int threadsPerGroup = 10;
        final Long betOfferId = 9001L;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        List<Future<StakeResult>> futures = new ArrayList<>();

        Set<Integer> stakeSet = new HashSet<>();
        Random random = new Random();
        while (stakeSet.size() < totalThreads) {
            stakeSet.add(random.nextInt(totalThreads*10) + 1);
        }
        List<Integer> uniqueStakes = new ArrayList<>(stakeSet);

        for (int i = 0; i < totalThreads / threadsPerGroup; i++) {
            final Long customerId = (long) (totalThreads + i);
            for (int j = 0; j < threadsPerGroup; j++) {
                int stakeIndex = i * threadsPerGroup + j;
                Future<StakeResult> future = executor.submit(new PlaceStakeTask(bettingHttpClient, customerId, betOfferId, uniqueStakes.get(stakeIndex)));
                futures.add(future);
            }
        }

        List<StakeResult> stakeResults = new ArrayList<>();
        Map<Long, Integer> highestStakesMap = new HashMap<>();

        int errorNums = 0;
        for (Future<StakeResult> future : futures) {
            try {
                StakeResult result = future.get();
                stakeResults.add(result);
                highestStakesMap.merge(result.customerId, result.stake, Math::max);
            } catch (Exception e) {
                System.out.println("Future Failed:" + e.getMessage());;
                errorNums++;
            }
        }
        System.out.println("errorNums:" + errorNums);
        List<StakeResult> uniqueStakeResults = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : highestStakesMap.entrySet()) {
            uniqueStakeResults.add(new StakeResult(entry.getKey(), entry.getValue()));
        }

        Collections.sort(uniqueStakeResults);

        int numberOfResultsToPrint = Math.min(20, uniqueStakeResults.size());

        String currentHighestStake = bettingHttpClient.getHighestStake(betOfferId);
        StringBuilder actualHighestStake = new StringBuilder();
        for (int i = 0; i < numberOfResultsToPrint; i++) {
            StakeResult result = uniqueStakeResults.get(i);
            actualHighestStake.append(String.format("%d=%d,", result.customerId, result.stake));
        }

        if (actualHighestStake.length() > 0) {
            actualHighestStake.deleteCharAt(actualHighestStake.length() - 1);
        }

        assert actualHighestStake.toString().equals(currentHighestStake);
        executor.shutdown();
    }

    private static class PlaceStakeTask implements Callable<StakeResult> {
        private final BettingHttpClient client;
        private final Long customerId;
        private final Long betOfferId;
        private final int stake;

        public PlaceStakeTask(BettingHttpClient client, Long customerId, Long betOfferId, int stake) {
            this.client = client;
            this.customerId = customerId;
            this.betOfferId = betOfferId;
            this.stake = stake;
        }

        @Override
        public StakeResult call() throws IOException {
            String sessionKey = client.getSession(customerId);
            client.postStake(betOfferId, sessionKey, stake);
            return new StakeResult(customerId, stake);
        }
    }

    private static class StakeResult implements Comparable<StakeResult> {
        private final Long customerId;
        private final int stake;

        public StakeResult(Long customerId, int stake) {
            this.customerId = customerId;
            this.stake = stake;
        }

        @Override
        public int compareTo(StakeResult o) {
            return Integer.compare(o.stake, this.stake); // Sort in descending order by stake
        }

        @Override
        public String toString() {
            return customerId + "=" + stake;
        }
    }


}
