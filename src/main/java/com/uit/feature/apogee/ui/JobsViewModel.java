package com.uit.feature.apogee.ui;

import com.uit.feature.apogee.job.DownloadProgressed;
import com.uit.feature.apogee.job.ExportSettings;
import com.uit.feature.apogee.job.ExportSettingsRepository;
import com.uit.feature.apogee.job.PartitionRule;
import com.uit.feature.apogee.job.PartitionRuleRepository;
import com.uit.feature.apogee.job.JobResult;
import com.uit.feature.apogee.job.JobResultRepository;
import com.uit.feature.apogee.job.JobScheduleRepository;
import com.uit.feature.apogee.job.JobScheduler;
import com.uit.feature.apogee.job.JobScript;
import com.uit.feature.apogee.job.SchedulePlan;
import com.uit.feature.apogee.job.ScheduleSlot;
import com.uit.feature.apogee.job.ScriptResultLine;
import com.uit.feature.apogee.job.TablespaceRule;
import com.uit.feature.apogee.job.TablespaceRuleRepository;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class JobsViewModel {

    private static final DateTimeFormatter SHOWN_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final List<JobScript> scripts;
    private final JobResultRepository results;
    private final JobScheduleRepository schedules;
    private final TablespaceRuleRepository tablespaceRules;
    private final PartitionRuleRepository partitionRules;
    private final ExportSettingsRepository exportSettings;
    private final JobScheduler scheduler;
    private final ObjectProperty<DownloadProgressed> download = new SimpleObjectProperty<>();
    private final ObservableList<JobRow> rows = FXCollections.observableArrayList();

    public JobsViewModel(
            List<JobScript> scripts,
            JobResultRepository results,
            JobScheduleRepository schedules,
            TablespaceRuleRepository tablespaceRules,
            PartitionRuleRepository partitionRules,
            ExportSettingsRepository exportSettings,
            JobScheduler scheduler
    ) {
        this.scripts = List.copyOf(scripts);
        this.results = results;
        this.schedules = schedules;
        this.tablespaceRules = tablespaceRules;
        this.partitionRules = partitionRules;
        this.exportSettings = exportSettings;
        this.scheduler = scheduler;
    }

    public ObjectProperty<DownloadProgressed> download() {
        return download;
    }

    public void showDownload(DownloadProgressed progress) {
        download.set(progress);
    }

    public void clearDownload() {
        download.set(null);
    }

    public ExportSettings exportSettings() {
        return exportSettings.load();
    }

    public void saveExportSettings(ExportSettings settings) {
        exportSettings.save(settings);
    }

    public ObservableList<JobRow> rows() {
        return rows;
    }

    public void executeNow(String scriptId) {
        scheduler.runNow(scriptId);
    }

    public List<ScheduleSlot> schedule(String scriptId) {
        JobScript script = find(scriptId);
        return schedules.load(script.id(), script.schedule().slots());
    }

    public void saveSchedule(String scriptId, List<ScheduleSlot> slots) {
        schedules.save(scriptId, slots);
        refresh();
    }

    public List<TablespaceRule> tablespaceRules() {
        return tablespaceRules.load();
    }

    public void saveTablespaceRules(List<TablespaceRule> rules) {
        tablespaceRules.save(rules);
    }

    public List<PartitionRule> partitionRules() {
        return partitionRules.load();
    }

    public void savePartitionRules(List<PartitionRule> rules) {
        partitionRules.save(rules);
    }

    public void refresh() {
        List<JobRow> next = new ArrayList<>();
        for (JobScript script : scripts) {
            next.add(toRow(script, results.latest(script.id()).orElse(null)));
        }
        rows.setAll(next);
    }

    private JobRow toRow(JobScript script, JobResult latest) {
        if (latest == null) {
            return new JobRow(script.id(), script.name(), plan(script).label(), "", "", false, List.of());
        }
        return new JobRow(
                script.id(),
                script.name(),
                plan(script).label(),
                SHOWN_TIME.format(latest.executedAt()),
                latest.success() ? "Success" : "Failed",
                !latest.success(),
                ScriptResultLine.decode(latest.storedResult())
        );
    }

    private SchedulePlan plan(JobScript script) {
        return new SchedulePlan(schedules.load(script.id(), script.schedule().slots()));
    }

    private JobScript find(String scriptId) {
        for (JobScript script : scripts) {
            if (script.id().equals(scriptId)) {
                return script;
            }
        }
        throw new IllegalArgumentException("Unknown script: " + scriptId);
    }
}
