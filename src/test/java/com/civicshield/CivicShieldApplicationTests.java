package com.civicshield;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic application context load test.
 * Verifies that the Spring application context starts without errors.
 *
 * Before running: ensure MySQL is running and application.properties
 * has a valid spring.datasource.password configured.
 */
@SpringBootTest
@ActiveProfiles("test")
class CivicShieldApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the application context started successfully.
    }
}
