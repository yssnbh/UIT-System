package com.uit.feature.apogee.job;

import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.shared.exception.AppException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Checks today's and yesterday's export dumps, then keeps the three newest dumps on this PC.
 */
public final class CheckExportDumps implements JobScript {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    static final ScriptSchedule SCHEDULE = new ScriptSchedule.Daily(java.time.LocalTime.of(9, 0));

    private final RemoteDumpStore remote;
    private final ExportSettingsRepository configuration;

    public CheckExportDumps(RemoteDumpStore remote, ExportSettingsRepository configuration) {
        this.remote = remote;
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return "check-export-dumps";
    }

    @Override
    public String name() {
        return "Check export dumps";
    }

    @Override
    public ScriptSchedule schedule() {
        return SCHEDULE;
    }

    @Override
    public JobExecution execute(ApogeeSettings settings) {
        return execute(settings, (index, total, fraction) -> {
        });
    }

    @Override
    public JobExecution execute(ApogeeSettings settings, JobProgress progress) {
        if (settings.serverIp().isBlank()
                || settings.port().isBlank()
                || settings.rootUsername().isBlank()
                || settings.rootPassword().isBlank()) {
            return failed("In Settings, fill in Server IP, Port, Root username, and Root password");
        }
        ExportSettings export = configuration.load();
        if (!export.path().matches("/[A-Za-z0-9_./-]+") || export.path().contains("..")) {
            return failed("The export folder path is not valid");
        }
        try {
            List<RemoteDump> dumps = remote.list(settings, export.path()).stream()
                    .filter(file -> ExportNames.isDump(file.name()))
                    .toList();
            List<ScriptResultLine> lines = new ArrayList<>();
            boolean ready = checkDay(settings, export, dumps, LocalDate.now(), "Today", lines)
                    & checkDay(settings, export, dumps, LocalDate.now().minusDays(1), "Yesterday", lines);
            if (!ready) {
                return new JobExecution(false, lines);
            }
            lines.addAll(sync(settings, export, dumps, progress));
            boolean failed = lines.stream().anyMatch(line -> line.tone() == ResultTone.DANGER);
            return new JobExecution(!failed, lines);
        } catch (AppException exception) {
            return failed(exception.getMessage());
        }
    }

    private boolean checkDay(
            ApogeeSettings settings,
            ExportSettings export,
            List<RemoteDump> dumps,
            LocalDate day,
            String label,
            List<ScriptResultLine> lines
    ) {
        List<RemoteDump> found = dumps.stream().filter(file -> ExportNames.day(file.name()).orElse(null) != null
                && ExportNames.day(file.name()).orElseThrow().equals(day)).toList();
        if (found.isEmpty()) {
            lines.add(new ScriptResultLine(ResultTone.DANGER, label + " " + DAY.format(day) + " dump is missing"));
            return false;
        }
        boolean ready = true;
        for (RemoteDump dump : found) {
            if (dump.bytes() < export.expectedBytes()) {
                lines.add(new ScriptResultLine(
                        ResultTone.DANGER,
                        label + " " + dump.name() + " is " + gigabytes(dump.bytes()) + ", expected " + export.expectedGigabytes() + " GB"
                ));
                ready = false;
                continue;
            }
            String logPath = export.path().replaceAll("/+$", "") + "/" + ExportNames.logName(dump.name());
            try {
                var problem = ExportLogs.problem(remote.readText(settings, logPath));
                if (problem.isPresent()) {
                    lines.add(new ScriptResultLine(ResultTone.DANGER, label + " " + ExportNames.logName(dump.name()) + ": " + problem.get()));
                    ready = false;
                    continue;
                }
            } catch (AppException exception) {
                lines.add(new ScriptResultLine(ResultTone.DANGER, label + " log " + ExportNames.logName(dump.name()) + " could not be read"));
                ready = false;
                continue;
            }
            lines.add(new ScriptResultLine(ResultTone.OK, label + " " + dump.name() + "  " + gigabytes(dump.bytes())));
        }
        return ready;
    }

    private List<ScriptResultLine> sync(
            ApogeeSettings settings,
            ExportSettings export,
            List<RemoteDump> dumps,
            JobProgress progress
    ) {
        Path localFolder = Path.of(export.localPath());
        List<RemoteDump> newest = dumps.stream()
                .sorted(Comparator.comparing((RemoteDump file) -> ExportNames.day(file.name()).orElse(LocalDate.MIN)).reversed()
                        .thenComparing(RemoteDump::name, Comparator.reverseOrder()))
                .limit(3)
                .toList();
        try {
            Files.createDirectories(localFolder);
        } catch (IOException exception) {
            return List.of(new ScriptResultLine(ResultTone.DANGER, "Unable to create the local export folder"));
        }
        List<ScriptResultLine> lines = new ArrayList<>();
        List<RemoteDump> missing = new ArrayList<>();
        for (RemoteDump dump : newest) {
            if (Files.exists(localFolder.resolve(dump.name()))) {
                lines.add(new ScriptResultLine(ResultTone.OK, dump.name() + " is already on this PC"));
            } else {
                missing.add(dump);
            }
        }
        String folder = export.path().replaceAll("/+$", "");
        for (int index = 0; index < missing.size(); index++) {
            RemoteDump dump = missing.get(index);
            int number = index + 1;
            progress.report(number, missing.size(), 0);
            remote.download(settings, folder + "/" + dump.name(), localFolder.resolve(dump.name()), fraction ->
                    progress.report(number, missing.size(), fraction)
            );
            lines.add(new ScriptResultLine(ResultTone.OK, "Downloaded " + number + "/" + missing.size() + "  " + dump.name()));
        }
        try (var local = Files.list(localFolder)) {
            List<String> keep = newest.stream().map(RemoteDump::name).toList();
            local.filter(path -> ExportNames.isDump(path.getFileName().toString()))
                    .filter(path -> !keep.contains(path.getFileName().toString()))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            lines.add(new ScriptResultLine(ResultTone.OK, "Removed old file " + path.getFileName()));
                        } catch (IOException exception) {
                            lines.add(new ScriptResultLine(ResultTone.DANGER, "Unable to remove " + path.getFileName()));
                        }
                    });
        } catch (IOException exception) {
            lines.add(new ScriptResultLine(ResultTone.DANGER, "Unable to clean the local export folder"));
        }
        return lines;
    }

    private static String gigabytes(long bytes) {
        return String.format(Locale.ROOT, "%.1f GB", bytes / 1024d / 1024d / 1024d);
    }

    private static JobExecution failed(String message) {
        return new JobExecution(false, List.of(new ScriptResultLine(ResultTone.DANGER, message)));
    }
}
