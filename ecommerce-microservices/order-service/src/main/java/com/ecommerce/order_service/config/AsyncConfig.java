package com.ecommerce.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// @EnableAsync — activates Spring's async method execution
// Without this, @Async methods run synchronously (annotation is ignored)
@Configuration
@EnableAsync
public class AsyncConfig {

    // Custom executor for order-related async tasks
    // Named "orderExecutor" — referenced in @Async("orderExecutor")
    @Bean("orderExecutor")
    public Executor orderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // always-alive threads
        executor.setMaxPoolSize(5);       // max threads under load
        executor.setQueueCapacity(100);   // tasks queued when all threads busy
        executor.setThreadNamePrefix("order-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
