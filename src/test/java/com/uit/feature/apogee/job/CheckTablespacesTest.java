package com.uit.feature.apogee.job;

import com.uit.feature.settings.domain.ApogeeSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckTablespacesTest {

    @Test
    void marksATablespaceThatMissesItsLimitAsDanger() {
        List<ScriptResultLine> lines = CheckTablespaces.format(List.of(
                new String[] {"SYSTEM", "/u01/oradata/system01.dbf", "512", "2048", "1536"},
                new String[] {"USERS", "/u01/oradata/users01.dbf", "3000", "3248", "248"},
                new String[] {"DATA_BO", "/u01/oradata/data_bo01.dbf", "900", "1000", "100"},
                new String[] {"DATA_APO", "/u01/oradata/data_apo01.dbf", "100", "4096", "3996"},
                new String[] {"DATA_APO", "/u01/oradata/data_apo02.dbf", "3000", "3200", "200"}
        ), TablespaceRules.defaults());

        assertEquals(5, lines.size());
        assertEquals(ResultTone.OK, lines.get(0).tone());
        assertTrue(lines.get(0).text().startsWith("SYSTEM  system01.dbf"));
        assertEquals(ResultTone.DANGER, lines.get(1).tone());
        assertEquals(ResultTone.DANGER, lines.get(2).tone());
        assertEquals(ResultTone.OK, lines.get(3).tone());
        assertEquals(ResultTone.DANGER, lines.get(4).tone());
    }

    @Test
    void reportsMissingDatabaseSettingsWithoutConnecting() {
        CheckTablespaces script = new CheckTablespaces((settings, sql) -> {
            throw new AssertionError("should not connect");
        }, new TablespaceRuleRepository() {
            @Override
            public List<TablespaceRule> load() {
                throw new AssertionError("should not load rules");
            }

            @Override
            public void save(List<TablespaceRule> rules) {
            }
        });

        JobExecution execution = script.execute(ApogeeSettings.empty());

        assertFalse(execution.success());
        assertEquals(ResultTone.DANGER, execution.lines().getFirst().tone());
    }
}
