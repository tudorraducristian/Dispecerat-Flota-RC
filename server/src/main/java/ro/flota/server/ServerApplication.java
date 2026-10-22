package ro.flota.server;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Dispecerul: ascultă flota prin Mosquitto, ține registrul în MySQL și împinge datele live spre UI. */
@SpringBootApplication
@EnableScheduling
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    /** Ceasul serverului, pe UTC. Clasele care au nevoie de oră îl primesc, ca testele să-l poată fixa. */
    @Bean
    Clock ceas() {
        return Clock.systemUTC();
    }
}
