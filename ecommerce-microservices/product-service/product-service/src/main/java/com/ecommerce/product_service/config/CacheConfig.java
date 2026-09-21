package com.ecommerce.product_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Cache configuration — Redis in non-test profiles, in-memory for tests.
 *
 * Cache-aside pattern (what @Cacheable implements):
 * 1. Check cache → if hit, return cached value (no DB call)
 * 2. If miss → call DB, store result in cache, return result
 * 3. On write → evict cache entry (@CacheEvict) so next read fetches fresh data
 *
 * TTL (Time To Live):
 * - Entries expire after 5 minutes automatically
 * - Prevents stale data from living in cache forever
 * - Trade-off: shorter TTL = fresher data but more DB calls
 *
 * Redis vs ConcurrentHashMap:
 * - Redis: shared across all service instances, survives restarts, supports TTL
 * - ConcurrentHashMap: per-instance, lost on restart, no TTL (manual eviction only)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    // --- Redis beans (active in all profiles except "test") ---

    @Bean
    @Profile("!test")
    public LettuceConnectionFactory redisConnectionFactory() {
        // Lettuce is the default Redis client — async, non-blocking, thread-safe
        // One connection shared across all threads (unlike Jedis which uses a pool)
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    @Bean
    @Profile("!test")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // Keys as plain strings, values as JSON
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    @Profile("!test")
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))          // entries expire after 5 minutes
                .disableCachingNullValues()                // don't cache null (avoids null-poisoning)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }

    // --- Fallback: in-memory cache for test profile (no Redis needed) ---

    @Bean
    @Profile("test")
    public CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager("products");
    }
}
