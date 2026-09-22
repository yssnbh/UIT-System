package com.uit.feature.home.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShowHomeTest {

    @Test
    void returnsTheWelcomeMessage() {
        assertEquals("Welcome back, yssn.", new ShowHome("yssn").execute());
    }
}
