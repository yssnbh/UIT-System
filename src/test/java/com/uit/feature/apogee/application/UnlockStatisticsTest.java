package com.uit.feature.apogee.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnlockStatisticsTest {

    @Test
    void countsUnlockedMissingAndFailedAccounts() {
        UnlockStatistics statistics = UnlockStatistics.from(List.of(
                new AccountUnlockResult("YSSN", UnlockOutcome.UNLOCKED, "Unlocked and password changed"),
                new AccountUnlockResult("ALI", UnlockOutcome.NOT_FOUND, "User not exists"),
                new AccountUnlockResult("BAD", UnlockOutcome.ERROR, "failed")
        ));

        assertEquals(1, statistics.unlocked());
        assertEquals(1, statistics.missing());
        assertEquals(1, statistics.errors());
    }
}
