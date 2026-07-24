package com.treasury;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableMethodSecurity
@EnableScheduling
public class TreasuryApplication {

    public static void main(String[] args) {
        SpringApplication.run(TreasuryApplication.class, args);
    }
}
