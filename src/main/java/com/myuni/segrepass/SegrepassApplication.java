package com.myuni.segrepass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class SegrepassApplication {

    public static void main(String[] args) {
        SpringApplication.run(SegrepassApplication.class, args);
    }

}
