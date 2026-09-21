package com.ecommerce.api_gateway.config;

import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

/**
 * Gateway route configuration using Spring Cloud Gateway Server MVC (WebMVC, not WebFlux).
 *
 * Routes:
 *   /api/products/**  → lb://product-service     (port 8081)
 *   /api/orders/**    → lb://order-service        (port 8083)
 *   /api/auth/**      → lb://user-service         (port 8082)
 *   /api/users/**     → lb://user-service         (port 8082)
 *   /api/notifications/** → lb://notification-service (port 8084)
 *
 * lb:// prefix tells Spring Cloud LoadBalancer to resolve the service name
 * via Eureka and pick an instance using round-robin by default.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouterFunction<ServerResponse> productRoutes() {
        return GatewayRouterFunctions.route("product-service")
                .route(path("/api/products/**"), HandlerFunctions.http())
                .filter(lb("product-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> orderRoutes() {
        return GatewayRouterFunctions.route("order-service")
                .route(path("/api/orders/**"), HandlerFunctions.http())
                .filter(lb("order-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> userRoutes() {
        return GatewayRouterFunctions.route("user-service")
                .route(path("/api/auth/**").or(path("/api/users/**")), HandlerFunctions.http())
                .filter(lb("user-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> notificationRoutes() {
        return GatewayRouterFunctions.route("notification-service")
                .route(path("/api/notifications/**"), HandlerFunctions.http())
                .filter(lb("notification-service"))
                .build();
    }

    /**
     * CORS configuration — allows the frontend (React/Angular on localhost:3000)
     * to call the gateway without browser CORS errors.
     *
     * In production: replace allowedOrigins with your actual domain.
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // preflight cache: 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
