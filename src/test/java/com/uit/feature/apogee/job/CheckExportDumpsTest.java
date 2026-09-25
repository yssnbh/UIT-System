package com.uit.feature.apogee.job;

import com.uit.feature.settings.domain.ApogeeSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckExportDumpsTest {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final long SIZE = 15L * 1024 * 1024 * 1024;

    @TempDir
    Path folder;

    @Test
    void reportsAMissingDumpAndDoesNotDownload() {
        AtomicInteger downloads = new AtomicInteger();
        CheckExportDumps script = script(List.of(dump(LocalDate.now())), "Job successfully completed", downloads);

        JobExecution execution = script.execute(server());

        assertFalse(execution.success());
        assertTrue(execution.lines().stream().anyMatch(line -> line.text().contains("Yesterday")));
        assertEquals(0, downloads.get());
    }

    @Test
    void downloadsAMissingDumpAndRemovesFilesBeyondThreeDays() throws Exception {
        Files.createFile(folder.resolve(name(LocalDate.now().minusDays(5))));
        AtomicInteger downloads = new AtomicInteger();
        List<RemoteDump> dumps = List.of(
                dump(LocalDate.now()),
                dump(LocalDate.now().minusDays(1)),
                dump(LocalDate.now().minusDays(2))
        );
        CheckExportDumps script = script(dumps, "successfully completed", downloads);

        JobExecution execution = script.execute(server(), (index, total, fraction) -> {
        });

        assertTrue(execution.success());
        assertEquals(3, downloads.get());
        assertFalse(Files.exists(folder.resolve(name(LocalDate.now().minusDays(5)))));
        assertTrue(Files.exists(folder.resolve(name(LocalDate.now()))));
    }

    @Test
    void marksALogErrorAsAProblem() {
        CheckExportDumps script = script(
                List.of(dump(LocalDate.now()), dump(LocalDate.now().minusDays(1))),
                "ORA-39001: invalid argument",
                new AtomicInteger()
        );

        JobExecution execution = script.execute(server());

        assertFalse(execution.success());
        assertTrue(execution.lines().stream().anyMatch(line -> line.tone() == ResultTone.DANGER && line.text().contains("ORA-39001")));
    }

    @Test
    void ignoresAnExportedTableWhoseNameContainsFail() {
        String log = """
                export : "SYSMAN"."MGMT_FAILOVER_CALLBACKS" 5.179 KB 4 lignes
                Job "SYS"."EXPORT_FULL" successfully completed
                """;

        assertTrue(ExportLogs.problem(log).isEmpty());
    }

    private CheckExportDumps script(List<RemoteDump> dumps, String log, AtomicInteger downloads) {
        RemoteDumpStore store = new RemoteDumpStore() {
            @Override
            public List<RemoteDump> list(ApogeeSettings settings, String directory) {
                return dumps;
            }

            @Override
            public String readText(ApogeeSettings settings, String path) {
                return log;
            }

            @Override
            public void download(ApogeeSettings settings, String remotePath, Path localFile, java.util.function.DoubleConsumer fraction) {
                downloads.incrementAndGet();
                try {
                    Files.writeString(localFile, "dump");
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
                fraction.accept(1);
            }
        };
        return new CheckExportDumps(store, new ExportSettingsRepository() {
            @Override
            public ExportSettings load() {
                return new ExportSettings("/fra/sauvegarde/dmp", 15, folder.toString());
            }

            @Override
            public void save(ExportSettings settings) {
            }
        });
    }

    private static RemoteDump dump(LocalDate day) {
        return new RemoteDump(name(day), SIZE);
    }

    private static String name(LocalDate day) {
        return "Export-full-kenitra-" + DAY.format(day) + "-6h.dmp";
    }

    private static ApogeeSettings server() {
        return new ApogeeSettings(
                "10.0.0.1", "22", "root", "secret",
                "", "", "", "", "", "", "",
                "", "", ""
        );
    }
}
