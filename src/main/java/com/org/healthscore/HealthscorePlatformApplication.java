package com.org.healthscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for the Engineering Health Scoring Framework.
 * 
 * This is a configuration-driven framework where:
 * - Java defines contracts, operators, and boundaries
 * - MongoDB defines meaning, weights, and priorities
 */
@SpringBootApplication
public class HealthscorePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthscorePlatformApplication.class, args);
    }
}
