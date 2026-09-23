package com.uit.feature.apogee.infrastructure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApogeeQueriesTest {

    @Test
    void eachQueryHasAStableKey() {
        assertEquals("find-user", ApogeeQueries.byKey("find-user").key());
        assertEquals("unlock-account", ApogeeQueries.UNLOCK_ACCOUNT.key());
        assertEquals("change-password", ApogeeQueries.CHANGE_PASSWORD.key());
        assertEquals("commit", ApogeeQueries.COMMIT.key());
        assertEquals("lock-account", ApogeeQueries.LOCK_ACCOUNT.key());
        assertEquals("expire-password", ApogeeQueries.EXPIRE_PASSWORD.key());
        assertTrue(ApogeeQueries.FIND_USER.sql().contains("FROM dba_users"));
        assertTrue(ApogeeQueries.FIND_USER.sql().contains("username = ?"));
    }

    @Test
    void buildsUnlockStatementsWithAnUppercaseQuotedName() {
        assertEquals("ALTER USER \"YSSN\" ACCOUNT UNLOCK", ApogeeQueries.UNLOCK_ACCOUNT.statement("yssn", null));
        assertEquals(
                "ALTER USER \"YSSN\" IDENTIFIED BY \"123456\"",
                ApogeeQueries.CHANGE_PASSWORD.statement("yssn", "123456")
        );
        assertEquals("COMMIT", ApogeeQueries.COMMIT.sql());
        assertEquals("ALTER USER \"APO_BOUSSALA\" ACCOUNT LOCK", ApogeeQueries.LOCK_ACCOUNT.statement("apo_boussala", null));
        assertEquals(
                "ALTER USER \"APO_BOUSSALA\" PASSWORD EXPIRE",
                ApogeeQueries.EXPIRE_PASSWORD.statement("apo_boussala", null)
        );
    }

    @Test
    void rejectsAnAccountNameThatCouldChangeTheStatement() {
        assertThrows(RuntimeException.class, () -> ApogeeQueries.UNLOCK_ACCOUNT.statement("yssn;drop", null));
    }
}
