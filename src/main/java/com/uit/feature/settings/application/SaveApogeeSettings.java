package com.uit.feature.settings.application;

import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.feature.settings.domain.ApogeeSettingsRepository;

public final class SaveApogeeSettings {

    private final ApogeeSettingsRepository repository;

    public SaveApogeeSettings(ApogeeSettingsRepository repository) {
        this.repository = repository;
    }

    public void execute(ApogeeSettings settings) {
        repository.save(settings);
    }
}
