package com.evosome.poseloombackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PoseloomApplication {

    public static void main(String[] args) {
        SpringApplication.run(PoseloomApplication.class, args);
    }
}
