package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.ProductResponse;

// ProductClient — interface defining the contract for calling product-service
// In Day 23+, this would be a @FeignClient with Eureka load balancing.
// For Spring Boot 4.x compatibility we implement it with RestClient (Spring 6.1+),
// which is the modern replacement for RestTemplate and equivalent to Feign in concept.
public interface ProductClient {
    ProductResponse getProductById(Long id);
}
