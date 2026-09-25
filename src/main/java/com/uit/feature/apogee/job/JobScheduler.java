package com.uit.feature.apogee.job;

import com.uit.feature.settings.domain.ApogeeSettingsRepository;
import com.uit.shared.event.EventBus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class JobScheduler {

    private final List<JobScript> scripts;
    private final ApogeeSettingsRepository settings;
    private final JobResultRepository results;
    private final JobScheduleRepository schedules;
    private final EventBus events;
    private final Set<String> running = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "apogee-jobs");
        thread.setDaemon(true);
        return thread;
    });

    public JobScheduler(
            List<JobScript> scripts,
            ApogeeSettingsRepository settings,
            JobResultRepository results,
            JobScheduleRepository schedules,
            EventBus events
    ) {
        this.scripts = List.copyOf(scripts);
        this.settings = settings;
        this.results = results;
        this.schedules = schedules;
        this.events = events;
    }

    public void start() {
        executor.scheduleWithFixedDelay(this::runDue, 3, 60, TimeUnit.SECONDS);
    }

    public void runNow(String scriptId) {
        executor.execute(() -> {
            for (JobScript script : scripts) {
                if (script.id().equals(scriptId)) {
                    run(script);
                    return;
                }
            }
        });
    }

    public List<JobScript> scripts() {
        return scripts;
    }

    private void runDue() {
        LocalDateTime now = LocalDateTime.now();
        results.deleteOlderThan(now.minusDays(60));
        for (JobScript script : scripts) {
            LocalDateTime lastRun = results.latest(script.id()).map(JobResult::executedAt).orElse(null);
            SchedulePlan plan = new SchedulePlan(schedules.load(script.id(), script.schedule().slots()));
            if (!plan.isDue(now, lastRun)) {
                continue;
            }
            run(script);
        }
    }

    private void run(JobScript script) {
        if (!running.add(script.id())) {
            return;
        }
        try {
            JobExecution execution;
            try {
                execution = script.execute(settings.load(), (index, total, fraction) ->
                        events.publish(new DownloadProgressed(script.id(), index, total, fraction))
                );
            } catch (RuntimeException exception) {
                String message = exception.getMessage() == null ? "The script failed" : exception.getMessage();
                execution = new JobExecution(false, List.of(new ScriptResultLine(ResultTone.DANGER, message)));
            }
            results.save(new JobResult(
                    script.id(),
                    LocalDateTime.now(),
                    execution.success(),
                    ScriptResultLine.encode(execution.lines())
            ));
            events.publish(new JobsUpdated());
        } finally {
            running.remove(script.id());
        }
    }
}
