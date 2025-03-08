package com.everymatrix.service;

import com.everymatrix.config.AppConfig;
import com.everymatrix.model.StakeEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class BetOfferService {

    /**
     * Simulates a row-based storage structure in a database to quickly determine
     * if a user's current bet is their highest bet within a bet offer.
     * Outer key: customerId , Inner key: betOfferId, Inner value: TreeSet of stakes.
     */
    private final Map<Long, Map<Long, TreeSet<Integer>>> bets = new ConcurrentHashMap<>();

    /**
     * Caches the top 20 stakes (de-duplicated by user) for fast retrieval.
     * Key: betOfferId, Value: TreeSet of customerId-stake pairs, ordered by stake descending.
     */
    private final Map<Long, TreeSet<StakeEntry>> highStakesCache = new ConcurrentHashMap<>();

    /**
     * Caches Lock objects to avoid the overhead of creating them repeatedly.
     */
    private final ConcurrentHashMap<Long, ReentrantLock> customerLocks = new ConcurrentHashMap<>(10000);
    private final ConcurrentHashMap<Long, ReentrantLock> offerLocks = new ConcurrentHashMap<>(1000);

    /**
     * Places a stake for a given bet offer and customer.
     * 1. Adds stake (O(log n) for TreeSet insertion), locks by customerId.
     * 2. Updates highStakesCache if stake is the user's maximum in the offer (locks by betOfferId).
     *
     */
    public void placeStake(Long betOfferId, Long customerId, Integer stake) {
        if(betOfferId == null || customerId == null || stake == null ){
            throw new IllegalArgumentException("placeStake args should not be null");
        }
        customerLocks.putIfAbsent(customerId, new ReentrantLock());
        ReentrantLock customerLock = customerLocks.get(customerId);

        customerLock.lock();
        try {
            bets.putIfAbsent(customerId, new ConcurrentHashMap<>());
            Map<Long, TreeSet<Integer>> betOfferBets = bets.get(customerId);

            betOfferBets.putIfAbsent(betOfferId, new TreeSet<>());
            TreeSet<Integer> stakes = betOfferBets.get(betOfferId);

            stakes.add(stake);

            Integer maxStake = stakes.last();

            if (stake.equals(maxStake)) {
                updateMaxStakesByOffer(betOfferId, customerId, stake);
            }
        } finally {
            customerLock.unlock();
        }
    }

    /**
     * Retrieves the stakes for a given customer.
     * This method is only for testing place stake
     *
     * @return A map of bet offers and their corresponding stakes.
     */
    public Map<Long, TreeSet<Integer>> queryBetOfferStakes(Long customerId) {
        return bets.getOrDefault(customerId, Collections.emptyMap());
    }

    /**
     * Update high stakes list cache
     */
    private void updateMaxStakesByOffer(Long betOfferId, Long customerId, Integer stake) {
        offerLocks.putIfAbsent(betOfferId, new ReentrantLock());
        ReentrantLock offerLock = offerLocks.get(betOfferId);

        offerLock.lock();
        try {
            highStakesCache.putIfAbsent(betOfferId, new TreeSet<>());
            TreeSet<StakeEntry> maxStakes = highStakesCache.get(betOfferId);

            maxStakes.stream().filter(e -> e.getCustomerId().equals(customerId)).findFirst().ifPresent(maxStakes::remove);

            maxStakes.add(new StakeEntry(customerId, stake)); // O(log n)

            if (maxStakes.size() > AppConfig.highStakesSizeForBetOffer) {
                maxStakes.pollLast(); // O(log n) to remove smallest
            }
        } finally {
            offerLock.unlock();
        }
    }

    /**
     * Retrieves the stakes for a specific bet offer
     * Complexity: O(n) n is the number of stakes returned
     *
     * @return Stakes in descending order
     */
    public List<StakeEntry> queryStakes(Long betOfferId) {
        if(betOfferId == null){
            throw new IllegalArgumentException("queryStakes.betOfferId should not be null");
        }
        TreeSet<StakeEntry> maxStakes = highStakesCache.getOrDefault(betOfferId, new TreeSet<>());
        return new ArrayList<>(maxStakes);
    }
}