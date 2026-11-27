package com.healthdekho;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HealthDekhoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthDekhoApplication.class, args);
    }
}
