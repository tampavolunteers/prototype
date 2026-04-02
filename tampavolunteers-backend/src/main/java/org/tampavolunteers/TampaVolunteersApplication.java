package org.tampavolunteers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Tampa Volunteers platform.
 */
@SpringBootApplication
@EnableScheduling
public class TampaVolunteersApplication {

    public static void main(String[] args) {
        SpringApplication.run(TampaVolunteersApplication.class, args);
    }
}
