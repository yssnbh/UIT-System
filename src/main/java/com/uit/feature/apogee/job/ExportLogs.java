package com.uit.feature.apogee.job;

import java.util.Optional;
import java.util.regex.Pattern;

public final class ExportLogs {

    private static final Pattern HARMLESS = Pattern.compile("(?i).*(\\bno errors?\\b|\\b0 errors?\\b).*");
    private static final Pattern PROBLEM = Pattern.compile(
            "(?i).*(ora-\\d+|\\berreurs?\\b|\\berrors?\\b|\\bfail(?:ed|ures?|s|ing)?\\b|\\béchecs?\\b|\\bechecs?\\b|\\bfatal\\b).*"
    );

    private ExportLogs() {
    }

    public static Optional<String> problem(String content) {
        if (content == null || content.isBlank()) {
            return Optional.of("The log is empty");
        }
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || HARMLESS.matcher(trimmed).matches()) {
                continue;
            }
            if (PROBLEM.matcher(trimmed).matches()) {
                return Optional.of(trimmed);
            }
        }
        return Optional.empty();
    }
}
