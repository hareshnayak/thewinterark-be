package com.winterark.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WinterArkApplication {

    public static void main(String[] args) {
        SpringApplication.run(WinterArkApplication.class, args);
    }

}
