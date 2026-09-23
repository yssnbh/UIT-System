package com.uit.feature.apogee.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnlockAccountsTest {

    @Test
    void readsOneOrMoreUsernamesSeparatedBySemicolon() {
        assertEquals(0, UnlockAccounts.usernames("  ").size());
        assertEquals(0, UnlockAccounts.usernames("; ;").size());
        assertEquals(java.util.List.of("YSSN"), UnlockAccounts.usernames(" yssn ; "));
        assertEquals(java.util.List.of("YSSN", "ALI"), UnlockAccounts.usernames("yssn; ali"));
    }

    @Test
    void buttonNamesOneAccountOrSeveral() {
        assertEquals("Déverrouillage le compte", UnlockAccounts.buttonLabel(""));
        assertEquals("Déverrouillage le compte", UnlockAccounts.buttonLabel("yssn"));
        assertEquals("Déverrouillage les comptes", UnlockAccounts.buttonLabel("yssn;ali"));
    }
}
