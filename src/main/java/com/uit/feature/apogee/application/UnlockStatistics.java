package com.uit.feature.apogee.application;

import java.util.List;

public record UnlockStatistics(int unlocked, int missing, int errors) {

    public static UnlockStatistics from(List<AccountUnlockResult> results) {
        int unlocked = 0;
        int missing = 0;
        int errors = 0;
        for (AccountUnlockResult result : results) {
            switch (result.outcome()) {
                case UNLOCKED -> unlocked++;
                case NOT_FOUND -> missing++;
                case ERROR -> errors++;
            }
        }
        return new UnlockStatistics(unlocked, missing, errors);
    }
}
