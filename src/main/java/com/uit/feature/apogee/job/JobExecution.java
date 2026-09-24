package com.uit.feature.apogee.job;

import java.util.List;

public record JobExecution(boolean success, List<ScriptResultLine> lines) {
}
