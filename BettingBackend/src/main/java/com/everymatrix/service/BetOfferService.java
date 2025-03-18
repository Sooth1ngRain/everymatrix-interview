package com.everymatrix.service;

import com.everymatrix.config.AppConfig;
import com.everymatrix.model.StakeEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class BetOfferService {

    /**
     * Caches the top 20 + 1 stakes (de-duplicated by user) for fast retrieval.
     * Key: betOfferId, Value: ConcurrentSkipListSet of customerId-stake pairs, ordered by stake descending.
     */
    private final Map<Integer, ConcurrentSkipListSet<StakeEntry>> highStakesCache = new ConcurrentHashMap<>();

    /**
     * Caches Lock objects to avoid the overhead of creating them repeatedly.
     */
    private final Map<Integer, ReentrantLock> customerLocks = new ConcurrentHashMap<>(10000);

    /**
     * Places a stake for a given bet offer and customer.
     * 1. Adds stake (O(log n) for TreeSet insertion), locks by customerId.
     * 2. Updates highStakesCache if stake is the user's maximum in the offer (locks by betOfferId).
     */
    public void placeStake(Integer betOfferId, Integer customerId, Integer stake) {
        if(betOfferId == null || customerId == null || stake == null ){
            throw new IllegalArgumentException("placeStake args should not be null");
        }
        highStakesCache.putIfAbsent(betOfferId, new ConcurrentSkipListSet<>());
        ConcurrentSkipListSet<StakeEntry> maxStakes = highStakesCache.get(betOfferId);

        ReentrantLock customerLock = customerLocks.computeIfAbsent(customerId, key -> new ReentrantLock());
        customerLock.lock();
        try {
            Optional<StakeEntry> optional = maxStakes.stream().filter(e -> e.getCustomerId() == customerId).findFirst();
            // remove the cached customer stake if customer's newly placed stake is higher
            if(optional.isPresent()){
                StakeEntry cachedStake = optional.get();
                if(cachedStake.getStake() < stake){
                    maxStakes.remove(cachedStake);
                } else {
                    return;
                }
            }
            maxStakes.add(new StakeEntry(customerId, stake));
            // +1 can ensure remove stakes without falling below AppConfig.highStakesSizeForBetOffer
            if (maxStakes.size() > AppConfig.highStakesSizeForBetOffer + 1) {
                maxStakes.pollLast(); // O(log n) to remove smallest
            }
        } finally {
            customerLock.unlock();
        }

    }

    /**
     * Retrieves the stakes for a specific bet offer
     * Complexity: O(n) n is the number of stakes returned
     *
     * @return Stakes in descending order
     */
    public List<StakeEntry> queryStakes(Integer betOfferId) {
        if (betOfferId == null) {
            throw new IllegalArgumentException("queryStakes.betOfferId should not be null");
        }
        ConcurrentSkipListSet<StakeEntry> maxStakes = highStakesCache.getOrDefault(betOfferId, new ConcurrentSkipListSet<>());
        return maxStakes.stream().limit(AppConfig.highStakesSizeForBetOffer).collect(Collectors.toList());
    }
}