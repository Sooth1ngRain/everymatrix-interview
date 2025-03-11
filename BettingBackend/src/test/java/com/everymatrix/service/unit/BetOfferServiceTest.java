package com.everymatrix.service.unit;

import com.everymatrix.model.StakeEntry;
import com.everymatrix.service.BetOfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class BetOfferServiceTest {

    private BetOfferService betOfferService;

    @BeforeEach
    public void setUp() {
        betOfferService = new BetOfferService();
    }

    @Test
    public void testPlaceStakeAndQueryStakes() {
        int betOfferId1 = 1;
        int betOfferId2 = 2;
        int customerId1 = 1001;
        int customerId2 = 1002;

        betOfferService.placeStake(betOfferId1, customerId1, 100);
        betOfferService.placeStake(betOfferId1, customerId2, 300);
        betOfferService.placeStake(betOfferId1, customerId1, 500);
        betOfferService.placeStake(betOfferId2, customerId1, 400);


        List<StakeEntry> topStakes = betOfferService.queryStakes(betOfferId1);

        assertEquals(2, topStakes.size());
        assertEquals(new StakeEntry(customerId1, 500), topStakes.get(0));
        assertEquals(new StakeEntry(customerId2, 300), topStakes.get(1));
        assertEquals(new StakeEntry(customerId1, 400), betOfferService.queryStakes(betOfferId2).get(0));

    }

    @Test
    public void testQueryCustomerStakes() {
        int betOfferId1 = 1;
        int betOfferId2 = 2;
        int customerId1 = 1;

        betOfferService.placeStake(betOfferId1, customerId1, 100);
        betOfferService.placeStake(betOfferId1, customerId1, 200);
        betOfferService.placeStake(betOfferId2, customerId1, 300);

        Map<Integer, TreeSet<Integer>> customerStakes = betOfferService.queryBetOfferStakes(customerId1);

        assertEquals(2, customerStakes.size());
        assertTrue(customerStakes.containsKey(betOfferId1));
        assertTrue(customerStakes.containsKey(betOfferId2));

        TreeSet<Integer> expectedStakesForBetOfferId1 = new TreeSet<>();
        expectedStakesForBetOfferId1.add(100);
        expectedStakesForBetOfferId1.add(200);

        TreeSet<Integer> expectedStakesForBetOfferId2 = new TreeSet<>();
        expectedStakesForBetOfferId2.add(300);

        assertEquals(expectedStakesForBetOfferId1, customerStakes.get(betOfferId1));
        assertEquals(expectedStakesForBetOfferId2, customerStakes.get(betOfferId2));
    }

    @Test
    public void testConcurrentPlaceStake() throws InterruptedException {
        Integer betOfferId = 1;
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    Integer customerId = threadIndex;
                    Integer stakeAmount = 100 * (threadIndex + 1);
                    betOfferService.placeStake(betOfferId, customerId, stakeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        List<StakeEntry> topStakes = betOfferService.queryStakes(betOfferId);

        assertEquals(20, topStakes.size());

        StakeEntry maxEntry = topStakes.get(0);
        assertEquals(100 * numberOfThreads, maxEntry.getStake());
    }
}