package com.uit.feature.apogee.job;

@FunctionalInterface
public interface JobProgress {

    void report(int index, int total, double fraction);
}
