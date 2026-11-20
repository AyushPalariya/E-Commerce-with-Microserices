package com.example.AuthService.Configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EnvTest implements CommandLineRunner {

    @Override
    public void run(String... args) {

        System.out.println("========== ENV TEST ==========");

        System.out.println(
                "DB URL: " + System.getenv("AUTH_DB_URL")
        );

        System.out.println(
                "JWT SECRET EXISTS: " +
                        (System.getenv("JWT_SECRET") != null)
        );

        System.out.println(
                "REDIS HOST: " + System.getenv("REDIS_HOST")
        );
        System.out.println("APIGATEWAY URL "+ System.getenv("API_GATEWAY_URL"));

        System.out.println(
                "KAFKA SERVER: " +
                        System.getenv("KAFKA_BOOTSTRAP_SERVERS")
        );

        System.out.println("==============================");
    }
}
