package com.utilityexplorer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UtilityExplorerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UtilityExplorerApplication.class, args);
    }
}