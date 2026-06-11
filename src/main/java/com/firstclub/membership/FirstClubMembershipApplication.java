package com.firstclub.membership;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FirstClubMembershipApplication {
    public static void main(String[] args) {
        SpringApplication.run(FirstClubMembershipApplication.class, args);
    }
}
