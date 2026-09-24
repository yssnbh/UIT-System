package com.uit.feature.apogee.job;

import java.util.ArrayList;
import java.util.List;

public record ScriptResultLine(ResultTone tone, String text) {

    public ScriptResultLine {
        text = text == null ? "" : text.replace('\n', ' ').replace('\t', ' ');
    }

    public static String encode(List<ScriptResultLine> lines) {
        StringBuilder stored = new StringBuilder();
        for (ScriptResultLine line : lines) {
            if (!stored.isEmpty()) {
                stored.append('\n');
            }
            stored.append(line.tone().name()).append('\t').append(line.text());
        }
        return stored.toString();
    }

    public static List<ScriptResultLine> decode(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        List<ScriptResultLine> lines = new ArrayList<>();
        for (String row : stored.split("\n", -1)) {
            int split = row.indexOf('\t');
            if (split < 0) {
                lines.add(new ScriptResultLine(ResultTone.NEUTRAL, row));
                continue;
            }
            ResultTone tone;
            try {
                tone = ResultTone.valueOf(row.substring(0, split));
            } catch (IllegalArgumentException exception) {
                tone = ResultTone.NEUTRAL;
            }
            lines.add(new ScriptResultLine(tone, row.substring(split + 1)));
        }
        return lines;
    }
}
