package com.uit.app.di;

import com.uit.app.navigation.Navigator;
import com.uit.app.navigation.Screen;
import com.uit.feature.apogee.application.LockApogeeAccounts;
import com.uit.feature.apogee.application.UnlockApogeeAccounts;
import com.uit.feature.apogee.infrastructure.OracleApogeeDatabase;
import com.uit.feature.apogee.ui.ApogeeScreen;
import com.uit.feature.home.application.ShowHome;
import com.uit.feature.home.ui.HomeScreen;
import com.uit.feature.home.ui.HomeViewModel;
import com.uit.feature.login.infrastructure.SqliteDatabase;
import com.uit.feature.settings.application.LoadApogeeSettings;
import com.uit.feature.settings.application.SaveApogeeSettings;
import com.uit.feature.settings.infrastructure.SqliteApogeeSettingsRepository;
import com.uit.feature.settings.ui.ApogeeSettingsViewModel;
import com.uit.feature.settings.ui.SettingsScreen;
import com.uit.shared.event.EventBus;

import java.util.List;

/**
 * Composition root. Register each new feature by adding its screen here.
 */
public final class AppContext {

    private final String signedInUsername;
    private final EventBus eventBus;
    private final Navigator navigator;

    public AppContext(String signedInUsername, SqliteDatabase database) {
        this.signedInUsername = signedInUsername;
        eventBus = new EventBus();
        navigator = new Navigator(List.of(homeScreen(), apogeeScreen(database), settingsScreen(database)));
    }

    public Navigator navigator() {
        return navigator;
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public String signedInUsername() {
        return signedInUsername;
    }

    private Screen homeScreen() {
        HomeViewModel viewModel = new HomeViewModel(new ShowHome(signedInUsername));
        return new HomeScreen(viewModel);
    }

    private static Screen apogeeScreen(SqliteDatabase database) {
        SqliteApogeeSettingsRepository repository = new SqliteApogeeSettingsRepository(database);
        UnlockApogeeAccounts unlock = new UnlockApogeeAccounts(repository, new OracleApogeeDatabase());
        LockApogeeAccounts lock = new LockApogeeAccounts(repository, new OracleApogeeDatabase());
        return new ApogeeScreen(unlock, lock);
    }

    private static Screen settingsScreen(SqliteDatabase database) {
        SqliteApogeeSettingsRepository repository = new SqliteApogeeSettingsRepository(database);
        ApogeeSettingsViewModel viewModel = new ApogeeSettingsViewModel(
                new LoadApogeeSettings(repository),
                new SaveApogeeSettings(repository)
        );
        return new SettingsScreen(viewModel);
    }
}
