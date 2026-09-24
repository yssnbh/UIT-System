package com.uit.feature.apogee.job;

import com.uit.feature.apogee.infrastructure.ApogeeQueries;
import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.shared.exception.AppException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads how full each Oracle tablespace is.
 */
public final class CheckTablespaces implements JobScript {

    static final ScriptSchedule SCHEDULE = new ScriptSchedule.DailyTimes(List.of(
            java.time.LocalTime.of(6, 0),
            java.time.LocalTime.of(12, 0),
            java.time.LocalTime.of(18, 0)
    ));

    private final OracleReader oracle;
    private final TablespaceRuleRepository rules;

    public CheckTablespaces(OracleReader oracle, TablespaceRuleRepository rules) {
        this.oracle = oracle;
        this.rules = rules;
    }

    @Override
    public String id() {
        return "check-tablespaces";
    }

    @Override
    public String name() {
        return "Check tablespaces";
    }

    @Override
    public ScriptSchedule schedule() {
        return SCHEDULE;
    }

    @Override
    public JobExecution execute(ApogeeSettings settings) {
        if (settings.databaseIp().isBlank()
                || settings.databasePort().isBlank()
                || settings.databaseSid().isBlank()
                || settings.sysPassword().isBlank()) {
            return failed("In Settings, fill in Database IP, Database port, Database SID, and Sys password");
        }
        try {
            List<ScriptResultLine> lines = format(
                    oracle.query(settings, ApogeeQueries.TABLESPACE_USAGE.sql()),
                    rules.load()
            );
            if (lines.isEmpty()) {
                return failed("The database did not report any tablespaces");
            }
            return new JobExecution(true, lines);
        } catch (AppException exception) {
            return failed(exception.getMessage());
        }
    }

    static List<ScriptResultLine> format(List<String[]> rows, List<TablespaceRule> rules) {
        List<ScriptResultLine> lines = new ArrayList<>();
        for (String[] row : rows) {
            if (row.length < 5 || row[0] == null || row[1] == null) {
                continue;
            }
            double used;
            double total;
            double free;
            try {
                used = Double.parseDouble(row[2]);
                total = Double.parseDouble(row[3]);
                free = Double.parseDouble(row[4]);
            } catch (NumberFormatException exception) {
                continue;
            }
            TablespaceRule rule = ruleFor(row[0], rules);
            boolean failed = rule != null && !rule.passes(free, total);
            String text = row[0] + "  " + fileName(row[1]) + "  " + size(used) + " used of " + size(total) + "  free " + size(free);
            lines.add(new ScriptResultLine(failed ? ResultTone.DANGER : ResultTone.OK, text));
        }
        return lines;
    }

    private static TablespaceRule ruleFor(String tablespace, List<TablespaceRule> rules) {
        for (TablespaceRule rule : rules) {
            if (rule.tablespace().equalsIgnoreCase(tablespace)) {
                return rule;
            }
        }
        return null;
    }

    private static String fileName(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private static String size(double megabytes) {
        if (megabytes >= 1024) {
            return String.format(Locale.ROOT, "%.1f GB", megabytes / 1024);
        }
        return String.format(Locale.ROOT, "%.0f MB", megabytes);
    }

    private static JobExecution failed(String message) {
        return new JobExecution(false, List.of(new ScriptResultLine(ResultTone.DANGER, message)));
    }
}
