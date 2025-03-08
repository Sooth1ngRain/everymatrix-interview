package com.everymatrix.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class StakeEntry implements Comparable<StakeEntry> {
    private final Long customerId;
    private final Integer stake;

    public StakeEntry(Long customerId, Integer stake) {
        this.customerId = customerId;
        this.stake = stake;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Integer getStake() {
        return stake;
    }

    @Override
    public int compareTo(StakeEntry other) {
        int stakeComparison = other.stake.compareTo(this.stake);
        return (stakeComparison != 0) ? stakeComparison : this.customerId.compareTo(other.customerId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StakeEntry that = (StakeEntry) o;
        return Objects.equals(customerId, that.customerId) &&
                Objects.equals(stake, that.stake);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, stake);
    }

    public static String convertToCSV(List<StakeEntry> stakeEntries) {
        // 使用流将每个 StakeEntry 转换为 "customerId=stake" 的格式

        // 将结果返回
        return stakeEntries.stream()
                .map(stakeEntry -> stakeEntry.customerId + "=" + stakeEntry.stake)
                .collect(Collectors.joining(","));
    }
}