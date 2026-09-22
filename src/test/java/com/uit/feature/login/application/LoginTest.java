package com.uit.feature.login.application;

import com.uit.feature.login.domain.User;
import com.uit.feature.login.domain.UserRepository;
import com.uit.feature.login.infrastructure.PasswordHasher;
import com.uit.feature.login.infrastructure.SqliteDatabase;
import com.uit.feature.login.infrastructure.SqliteUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginTest {

    @TempDir
    Path folder;

    @Test
    void seededUserSignsInWithPasswordOrEmail() {
        SqliteDatabase database = SqliteDatabase.open(folder.resolve("uit.db"));
        try {
            UserRepository users = new SqliteUserRepository(database);
            User user = new LoginWithPassword(users).execute("yssn", "yssnBH++09").orElseThrow();
            assertEquals("yssn", user.username());
            assertEquals("yassine.bouhroz@uit.ac.ma", user.email());
            assertTrue(new LoginWithPassword(users).execute("YSSN", "yssnBH++09").isPresent());
            assertTrue(new LoginWithPassword(users).execute("yssn", "wrong-password").isEmpty());
            assertTrue(new LoginWithPassword(users).execute("  ", "yssnBH++09").isEmpty());

            User google = new LoginWithGoogle(users).execute("Yassine.Bouhroz@uit.ac.ma").orElseThrow();
            assertEquals("yssn", google.username());
            assertTrue(new LoginWithGoogle(users).execute("someone@gmail.com").isEmpty());
        } finally {
            database.close();
        }
    }

    @Test
    void passwordHashDoesNotMatchADifferentPassword() {
        PasswordHasher hasher = new PasswordHasher();
        String stored = hasher.hash("yssnBH++09");
        assertFalse(stored.contains("yssnBH++09"));
        assertTrue(hasher.matches("yssnBH++09", stored));
        assertFalse(hasher.matches("other", stored));
        assertFalse(hasher.matches("yssnBH++09", "not-a-hash"));
    }

    @Test
    void googleRejectsAnUnverifiedEmail() {
        assertThrows(RuntimeException.class, () -> com.uit.feature.login.infrastructure.GoogleUserInfo.verifiedEmail(
                "{\"email\":\"yassine.bouhroz@uit.ac.ma\",\"email_verified\":false}"
        ));
    }

    @Test
    void googleReadsAVerifiedEmail() {
        assertEquals(
                "yassine.bouhroz@uit.ac.ma",
                com.uit.feature.login.infrastructure.GoogleUserInfo.verifiedEmail(
                        "{\"email\":\"yassine.bouhroz@uit.ac.ma\",\"email_verified\":true}"
                )
        );
    }

    @Test
    void blankLookupDoesNotQueryTheDatabase() {
        UserRepository users = new UserRepository() {
            @Override
            public Optional<User> authenticate(String username, String password) {
                throw new AssertionError("authenticate should not run");
            }

            @Override
            public Optional<User> findByEmail(String email) {
                throw new AssertionError("findByEmail should not run");
            }
        };
        assertTrue(new LoginWithPassword(users).execute("   ", "secret").isEmpty());
        assertTrue(new LoginWithGoogle(users).execute("  ").isEmpty());
    }
}
