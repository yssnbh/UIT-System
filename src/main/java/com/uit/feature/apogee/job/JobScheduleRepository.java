package com.uit.feature.apogee.job;

import java.util.List;

public interface JobScheduleRepository {

    List<ScheduleSlot> load(String scriptId, List<ScheduleSlot> defaults);

    void save(String scriptId, List<ScheduleSlot> slots);
}
