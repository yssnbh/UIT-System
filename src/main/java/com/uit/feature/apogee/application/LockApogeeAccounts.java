package com.uit.feature.apogee.application;

import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.feature.settings.domain.ApogeeSettingsRepository;
import com.uit.shared.exception.AppException;

import java.util.List;
import java.util.Locale;

public final class LockApogeeAccounts {

    private final ApogeeSettingsRepository settingsRepository;
    private final ApogeeDatabase database;

    public LockApogeeAccounts(ApogeeSettingsRepository settingsRepository, ApogeeDatabase database) {
        this.settingsRepository = settingsRepository;
        this.database = database;
    }

    public List<AccountUnlockResult> execute(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            throw new AppException("Enter at least one account name");
        }
        ApogeeSettings settings = settingsRepository.load();
        if (settings.databaseIp().isBlank()
                || settings.databasePort().isBlank()
                || settings.databaseSid().isBlank()
                || settings.sysPassword().isBlank()) {
            throw new AppException(
                    "In Settings, fill in Database IP, Database port, Database SID, and Sys password"
            );
        }
        List<String> accounts = usernames.stream()
                .map(name -> name == null ? "" : name.trim().toUpperCase(Locale.ROOT))
                .filter(name -> !name.isEmpty())
                .toList();
        if (accounts.isEmpty()) {
            throw new AppException("Enter at least one account name");
        }
        return database.lock(settings, accounts);
    }
}
