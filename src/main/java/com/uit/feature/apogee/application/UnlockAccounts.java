package com.uit.feature.apogee.application;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class UnlockAccounts {

    public static final String ONE_ACCOUNT = "Déverrouillage le compte";
    public static final String SEVERAL_ACCOUNTS = "Déverrouillage les comptes";

    private UnlockAccounts() {
    }

    public static List<String> usernames(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(";"))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> name.toUpperCase(Locale.ROOT))
                .toList();
    }

    public static String buttonLabel(String raw) {
        return usernames(raw).size() > 1 ? SEVERAL_ACCOUNTS : ONE_ACCOUNT;
    }
}
