package com.uit.feature.apogee.job;

import com.uit.feature.settings.domain.ApogeeSettings;

import java.nio.file.Path;
import java.util.List;
import java.util.function.DoubleConsumer;

public interface RemoteDumpStore {

    List<RemoteDump> list(ApogeeSettings settings, String directory);

    String readText(ApogeeSettings settings, String path);

    void download(ApogeeSettings settings, String remotePath, Path localFile, DoubleConsumer fraction);
}
