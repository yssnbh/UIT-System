package com.uit.feature.apogee.job;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExportNames {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final Pattern DUMP = Pattern.compile("Export-full-kenitra-(\\d{2}-\\d{2}-\\d{4})-(\\d{1,2})h\\.dmp");

    private ExportNames() {
    }

    public static boolean isDump(String name) {
        return name != null && DUMP.matcher(name).matches();
    }

    public static Optional<LocalDate> day(String name) {
        Matcher matcher = DUMP.matcher(name == null ? "" : name);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(LocalDate.parse(matcher.group(1), DAY));
    }

    public static String logName(String dumpName) {
        return dumpName.substring(0, dumpName.length() - 4) + ".log";
    }
}
