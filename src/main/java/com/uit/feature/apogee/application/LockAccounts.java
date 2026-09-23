package com.uit.feature.apogee.application;

public final class LockAccounts {

    public static final String ONE_ACCOUNT = "Verrouillage le compte";
    public static final String SEVERAL_ACCOUNTS = "Verrouillage les comptes";

    private LockAccounts() {
    }

    public static String buttonLabel(String raw) {
        return UnlockAccounts.usernames(raw).size() > 1 ? SEVERAL_ACCOUNTS : ONE_ACCOUNT;
    }
}
