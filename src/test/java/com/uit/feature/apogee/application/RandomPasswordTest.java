package com.uit.feature.apogee.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomPasswordTest {

    @Test
    void generatesAPasswordWithLettersAndDigits() {
        String password = RandomPassword.generate();

        assertEquals(12, password.length());
        assertTrue(password.chars().anyMatch(Character::isUpperCase));
        assertTrue(password.chars().anyMatch(Character::isLowerCase));
        assertTrue(password.chars().anyMatch(Character::isDigit));
        assertTrue(password.chars().allMatch(Character::isLetterOrDigit));
    }
}
