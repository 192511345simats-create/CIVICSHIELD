package com.civicshield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CivicShieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(CivicShieldApplication.class, args);
    }
}
