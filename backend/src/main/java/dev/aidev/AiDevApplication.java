package dev.aidev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiDevApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDevApplication.class, args);
    }
}
