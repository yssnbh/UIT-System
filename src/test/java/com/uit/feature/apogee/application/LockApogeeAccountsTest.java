package com.uit.feature.apogee.application;

import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.feature.settings.domain.ApogeeSettingsRepository;
import com.uit.shared.exception.AppException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LockApogeeAccountsTest {

    @Test
    void doesNotConnectWhenDatabaseSettingsAreMissing() {
        ApogeeDatabase database = new ApogeeDatabase() {
            @Override
            public List<AccountUnlockResult> unlock(ApogeeSettings settings, List<String> usernames, String password) {
                throw new AssertionError("unlock should not run");
            }

            @Override
            public List<AccountUnlockResult> lock(ApogeeSettings settings, List<String> usernames) {
                throw new AssertionError("database should not be opened");
            }
        };
        LockApogeeAccounts lock = new LockApogeeAccounts(repository(ApogeeSettings.empty()), database);

        assertThrows(AppException.class, () -> lock.execute(List.of("APO_BOUSSALA")));
    }

    @Test
    void sendsUppercaseAccountsWhenSettingsAreComplete() {
        ApogeeSettings stored = new ApogeeSettings(
                "", "", "", "", "", "", "", "", "sys-secret", "", "", "10.1.1.30", "1521", "APOGEE"
        );
        List<AccountUnlockResult> expected = List.of(
                new AccountUnlockResult("APO_BOUSSALA", UnlockOutcome.UNLOCKED, "Account locked")
        );
        ApogeeDatabase database = new ApogeeDatabase() {
            @Override
            public List<AccountUnlockResult> unlock(ApogeeSettings settings, List<String> usernames, String password) {
                throw new AssertionError("unlock should not run");
            }

            @Override
            public List<AccountUnlockResult> lock(ApogeeSettings settings, List<String> usernames) {
                assertEquals(List.of("APO_BOUSSALA"), usernames);
                return expected;
            }
        };

        assertEquals(expected, new LockApogeeAccounts(repository(stored), database).execute(List.of("apo_boussala")));
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
