package com.uit.feature.apogee.job;

import com.uit.feature.settings.domain.ApogeeSettings;

import java.util.List;

public interface OracleReader {

    List<String[]> query(ApogeeSettings settings, String sql);
}
