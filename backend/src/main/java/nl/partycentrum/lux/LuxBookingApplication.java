package nl.partycentrum.lux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LuxBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(LuxBookingApplication.class, args);
    }
}
