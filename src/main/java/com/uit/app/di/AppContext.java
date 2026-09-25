package com.uit.app.di;

import com.uit.app.navigation.Navigator;
import com.uit.app.navigation.Screen;
import com.uit.feature.apogee.application.LockApogeeAccounts;
import com.uit.feature.apogee.application.UnlockApogeeAccounts;
import com.uit.feature.apogee.infrastructure.OracleApogeeDatabase;
import com.uit.feature.apogee.job.CheckExportDumps;
import com.uit.feature.apogee.job.CheckServerSpace;
import com.uit.feature.apogee.job.CheckTablespaces;
import com.uit.feature.apogee.job.JdbcOracleReader;
import com.uit.feature.apogee.job.JobScheduler;
import com.uit.feature.apogee.job.SshDumpStore;
import com.uit.feature.apogee.job.SqliteExportSettingsRepository;
import com.uit.feature.apogee.job.SqlitePartitionRuleRepository;
import com.uit.feature.apogee.job.SqliteJobScheduleRepository;
import com.uit.feature.apogee.job.SqliteTablespaceRuleRepository;
import com.uit.feature.apogee.job.SshCommands;
import com.uit.feature.apogee.job.SqliteJobResultRepository;
import com.uit.feature.apogee.ui.JobsViewModel;
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
    private final JobScheduler jobScheduler;

    public AppContext(String signedInUsername, SqliteDatabase database) {
        this.signedInUsername = signedInUsername;
        eventBus = new EventBus();
        jobScheduler = jobs(database);
        jobScheduler.start();
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

    private Screen apogeeScreen(SqliteDatabase database) {
        SqliteApogeeSettingsRepository repository = new SqliteApogeeSettingsRepository(database);
        UnlockApogeeAccounts unlock = new UnlockApogeeAccounts(repository, new OracleApogeeDatabase());
        LockApogeeAccounts lock = new LockApogeeAccounts(repository, new OracleApogeeDatabase());
        SqliteJobScheduleRepository schedules = new SqliteJobScheduleRepository(database);
        JobsViewModel jobs = new JobsViewModel(
                jobScheduler.scripts(),
                new SqliteJobResultRepository(database),
                schedules,
                new SqliteTablespaceRuleRepository(database),
                new SqlitePartitionRuleRepository(database),
                new SqliteExportSettingsRepository(database),
                jobScheduler
        );
        return new ApogeeScreen(unlock, lock, jobs, eventBus);
    }

    private JobScheduler jobs(SqliteDatabase database) {
        SqliteApogeeSettingsRepository repository = new SqliteApogeeSettingsRepository(database);
        SqliteJobResultRepository results = new SqliteJobResultRepository(database);
        SqliteJobScheduleRepository schedules = new SqliteJobScheduleRepository(database);
        SqliteTablespaceRuleRepository tablespaceRules = new SqliteTablespaceRuleRepository(database);
        SqlitePartitionRuleRepository partitionRules = new SqlitePartitionRuleRepository(database);
        return new JobScheduler(List.of(
                new CheckServerSpace(new SshCommands(), partitionRules),
                new CheckTablespaces(new JdbcOracleReader(), tablespaceRules),
                new CheckExportDumps(new SshDumpStore(), new SqliteExportSettingsRepository(database))
        ), repository, results, schedules, eventBus);
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
