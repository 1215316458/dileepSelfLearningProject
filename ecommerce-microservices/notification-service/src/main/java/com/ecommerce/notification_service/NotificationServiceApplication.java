package com.ecommerce.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @EnableMongoAuditing is in MongoConfig (not here) so it can be excluded in tests
 * by mocking the repository — the auditing handler needs mongoMappingContext which
 * is only available when MongoDataAutoConfiguration is active.
 */
@SpringBootApplication
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
