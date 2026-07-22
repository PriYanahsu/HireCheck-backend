package com.hirecheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HireCheckApplication {
    public static void main(String[] args) {
        SpringApplication.run(HireCheckApplication.class, args);
    }
}
