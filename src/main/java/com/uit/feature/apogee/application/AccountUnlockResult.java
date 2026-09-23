package com.uit.feature.apogee.application;

public record AccountUnlockResult(String username, UnlockOutcome outcome, String message) {
}
