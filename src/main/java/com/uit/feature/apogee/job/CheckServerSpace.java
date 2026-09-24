package com.uit.feature.apogee.job;

import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.shared.exception.AppException;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the used space of every partition on the Apogee server.
 */
public final class CheckServerSpace implements JobScript {

    static final ScriptSchedule SCHEDULE = new ScriptSchedule.Daily(java.time.LocalTime.of(8, 0));

    private final RemoteShell shell;
    private final PartitionRuleRepository rules;

    public CheckServerSpace(RemoteShell shell, PartitionRuleRepository rules) {
        this.shell = shell;
        this.rules = rules;
    }

    @Override
    public String id() {
        return "check-server-space";
    }

    @Override
    public String name() {
        return "Check server space";
    }

    @Override
    public ScriptSchedule schedule() {
        return SCHEDULE;
    }

    @Override
    public JobExecution execute(ApogeeSettings settings) {
        if (settings.serverIp().isBlank()
                || settings.port().isBlank()
                || settings.rootUsername().isBlank()
                || settings.rootPassword().isBlank()) {
            return failed("In Settings, fill in Server IP, Port, Root username, and Root password");
        }
        int port;
        try {
            port = Integer.parseInt(settings.port());
        } catch (NumberFormatException exception) {
            return failed("Server port is not valid");
        }
        if (port < 1 || port > 65535) {
            return failed("Server port is not valid");
        }
        try {
            String output = shell.run(
                    settings.serverIp(),
                    port,
                    settings.rootUsername(),
                    settings.rootPassword(),
                    "df -Ph"
            );
            List<ScriptResultLine> lines = parse(output, rules.load());
            if (lines.isEmpty()) {
                return failed("The server did not report any partitions");
            }
            return new JobExecution(true, lines);
        } catch (AppException exception) {
            return failed(exception.getMessage());
        }
    }

    static List<ScriptResultLine> parse(String output, List<PartitionRule> rules) {
        List<ScriptResultLine> lines = new ArrayList<>();
        if (output == null) {
            return lines;
        }
        for (String row : output.split("\\R")) {
            String trimmed = row.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("Filesystem")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length < 6 || !parts[4].endsWith("%")) {
                continue;
            }
            int percent;
            try {
                percent = Integer.parseInt(parts[4].substring(0, parts[4].length() - 1));
            } catch (NumberFormatException exception) {
                continue;
            }
            String mount = String.join(" ", java.util.Arrays.copyOfRange(parts, 5, parts.length));
            String text = mount + "  " + parts[2] + " used of " + parts[1] + "  " + percent + "%";
            PartitionRule rule = ruleFor(mount, rules);
            boolean failed = rule != null && !rule.passes(percent);
            lines.add(new ScriptResultLine(failed ? ResultTone.DANGER : ResultTone.OK, text));
        }
        return lines;
    }

    private static PartitionRule ruleFor(String mount, List<PartitionRule> rules) {
        PartitionRule others = null;
        for (PartitionRule rule : rules) {
            if (rule.others()) {
                others = rule;
            } else if (rule.mount().equals(mount)) {
                return rule;
            }
        }
        return others;
    }

    private static JobExecution failed(String message) {
        return new JobExecution(false, List.of(new ScriptResultLine(ResultTone.DANGER, message)));
    }
}
