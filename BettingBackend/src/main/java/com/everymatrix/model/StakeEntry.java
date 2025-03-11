package com.everymatrix.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class StakeEntry implements Comparable<StakeEntry> {
    private final int customerId;
    private final int stake;

    public StakeEntry(int customerId, int stake) {
        this.customerId = customerId;
        this.stake = stake;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getStake() {
        return stake;
    }

    @Override
    public int compareTo(StakeEntry other) {
        return other.stake - this.stake;
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