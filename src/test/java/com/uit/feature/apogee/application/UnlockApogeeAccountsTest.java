package com.uit.feature.apogee.application;

import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.feature.settings.domain.ApogeeSettingsRepository;
import com.uit.shared.exception.AppException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnlockApogeeAccountsTest {

    @Test
    void doesNotConnectWhenDatabaseSettingsAreMissing() {
        ApogeeDatabase database = new ApogeeDatabase() {
            @Override
            public List<AccountUnlockResult> unlock(ApogeeSettings settings, List<String> usernames, String password) {
                throw new AssertionError("database should not be opened");
            }

            @Override
            public List<AccountUnlockResult> lock(ApogeeSettings settings, List<String> usernames) {
                throw new AssertionError("lock should not run");
            }
        };
        UnlockApogeeAccounts unlock = new UnlockApogeeAccounts(repository(ApogeeSettings.empty()), database);

        assertThrows(AppException.class, () -> unlock.execute(List.of("yssn"), "123456"));
    }

    @Test
    void sendsTheAccountsAndPasswordWhenSettingsAreComplete() {
        ApogeeSettings stored = new ApogeeSettings(
                "", "", "", "", "", "oracle-secret", "", "", "sys-secret", "", "", "10.1.1.30", "1521", "APOGEE"
        );
        List<AccountUnlockResult> expected = List.of(
                new AccountUnlockResult("YSSN", UnlockOutcome.UNLOCKED, "ok")
        );
        ApogeeDatabase database = new ApogeeDatabase() {
            @Override
            public List<AccountUnlockResult> unlock(ApogeeSettings settings, List<String> usernames, String password) {
                assertEquals("sys-secret", settings.sysPassword());
                assertEquals("10.1.1.30", settings.databaseIp());
                assertEquals(List.of("YSSN", "ALI"), usernames);
                assertEquals("123456", password);
                return expected;
            }

            @Override
            public List<AccountUnlockResult> lock(ApogeeSettings settings, List<String> usernames) {
                throw new AssertionError("lock should not run");
            }
        };

        List<AccountUnlockResult> results = new UnlockApogeeAccounts(repository(stored), database)
                .execute(List.of("yssn", "ali"), "123456");

        assertEquals(expected, results);
    }

    private static ApogeeSettingsRepository repository(ApogeeSettings settings) {
        return new ApogeeSettingsRepository() {
            @Override
            public ApogeeSettings load() {
                return settings;
            }

            @Override
            public void save(ApogeeSettings ignored) {
            }
        };
    }
}
