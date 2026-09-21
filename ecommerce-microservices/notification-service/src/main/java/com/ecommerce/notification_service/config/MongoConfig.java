package com.ecommerce.notification_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * @EnableMongoAuditing is in a @Profile("!test") config so tests that exclude
 * MongoDataAutoConfiguration don't fail trying to resolve mongoMappingContext.
 */
@Configuration
@Profile("!test")
@EnableMongoAuditing
public class MongoConfig {
}
