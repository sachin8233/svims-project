package com.svims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Smart Vendor & Invoice Management System
 * 
 * @EnableScheduling enables scheduled tasks for reminders and escalations
 */
@SpringBootApplication
@EnableScheduling
public class SvimsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SvimsApplication.class, args);
    }
}

