package com.everymatrix.service.unit;

import com.everymatrix.model.StakeEntry;
import com.everymatrix.service.BetOfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    /**
     * Concurrent Stake Placement by Multiple Customers (100 customer place stake at the same time , each customer place 10 stakes simultaneously)
     */
    @Test
    public void testConcurrentPlaceStake() throws InterruptedException {
        Integer betOfferId = 1;
        int numberOfClients = 100;
        int stakesPerClient = 10;
        ExecutorService mainExecutorService = Executors.newFixedThreadPool(numberOfClients);
        CountDownLatch latch = new CountDownLatch(numberOfClients * stakesPerClient);

        Random random = new Random();

        Map<Integer, Integer> customerMaxStakes = new ConcurrentHashMap<>();

        List<Integer> indices = IntStream.range(0, numberOfClients).boxed().collect(Collectors.toList());
        Collections.shuffle(indices);

        for (int i = 0; i < numberOfClients; i++) {
            final int clientIndex = indices.get(i);
            mainExecutorService.submit(() -> {
                Integer customerId = clientIndex;
                ExecutorService clientExecutorService = Executors.newFixedThreadPool(stakesPerClient);

                for (int j = 0; j < stakesPerClient; j++) {
                    clientExecutorService.submit(() -> {
                        try {
                            Integer stakeAmount = random.nextInt(100_000_001);
                            betOfferService.placeStake(betOfferId, customerId, stakeAmount);

                            customerMaxStakes.merge(customerId, stakeAmount, Math::max);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                clientExecutorService.shutdown();
                try {
                    clientExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.await();
        mainExecutorService.shutdown();

        List<StakeEntry> topStakes = betOfferService.queryStakes(betOfferId);

        assertEquals(20, topStakes.size());

        topStakes.sort(Comparator.comparingInt(StakeEntry::getStake).reversed());

        List<Map.Entry<Integer, Integer>> expectedTopStakes = customerMaxStakes.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(20)
                .collect(Collectors.toList());


        for (int i = 0; i < 20; i++) {
            assertEquals(expectedTopStakes.get(i).getValue(), topStakes.get(i).getStake());
        }
    }
}