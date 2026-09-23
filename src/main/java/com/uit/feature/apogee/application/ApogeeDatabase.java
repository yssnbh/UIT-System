package com.uit.feature.apogee.application;

import com.uit.feature.settings.domain.ApogeeSettings;

import java.util.List;

public interface ApogeeDatabase {

    List<AccountUnlockResult> unlock(ApogeeSettings settings, List<String> usernames, String password);

    List<AccountUnlockResult> lock(ApogeeSettings settings, List<String> usernames);
}
