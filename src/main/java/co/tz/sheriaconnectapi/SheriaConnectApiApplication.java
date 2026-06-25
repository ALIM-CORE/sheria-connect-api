package co.tz.sheriaconnectapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SheriaConnectApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SheriaConnectApiApplication.class, args);
    }

}
