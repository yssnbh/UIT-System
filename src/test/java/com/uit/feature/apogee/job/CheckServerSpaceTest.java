package com.uit.feature.apogee.job;

import com.uit.feature.settings.domain.ApogeeSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CheckServerSpaceTest {

    @Test
    void marksAPartitionAboveItsLimitAsDanger() {
        String output = """
                Filesystem      Size  Used Avail Use% Mounted on
                /dev/sda1        50G   46G  4.1G  92% /
                /dev/sdb1       200G  180G   20G  90% /data
                /dev/sdc1       100G   61G   39G  61% /fra
                /dev/sdd1       100G   10G   90G  10% /u01
                """;

        List<ScriptResultLine> lines = CheckServerSpace.parse(output, PartitionRules.defaults());

        assertEquals(4, lines.size());
        assertEquals(ResultTone.DANGER, lines.get(0).tone());
        assertEquals(ResultTone.OK, lines.get(1).tone());
        assertEquals(ResultTone.DANGER, lines.get(2).tone());
        assertEquals(ResultTone.OK, lines.get(3).tone());
    }

    @Test
    void reportsMissingServerSettingsWithoutConnecting() {
        CheckServerSpace script = new CheckServerSpace((host, port, username, password, command) -> {
            throw new AssertionError("should not connect");
        }, new PartitionRuleRepository() {
            @Override
            public List<PartitionRule> load() {
                throw new AssertionError("should not load rules");
            }

            @Override
            public void save(List<PartitionRule> rules) {
            }
        });

        JobExecution execution = script.execute(ApogeeSettings.empty());

        assertFalse(execution.success());
        assertEquals(ResultTone.DANGER, execution.lines().getFirst().tone());
    }
}
