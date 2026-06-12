package com.kabooz.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Kabooz Goli Soda backend application.
 * <p>
 * Sri Rama Krupa Enterprises — Production-ready Spring Boot 3.2.x backend.
 * </p>
 */
@SpringBootApplication
@EnableScheduling
public class KaboozApplication {

    public static void main(String[] args) {
        SpringApplication.run(KaboozApplication.class, args);
    }
}
