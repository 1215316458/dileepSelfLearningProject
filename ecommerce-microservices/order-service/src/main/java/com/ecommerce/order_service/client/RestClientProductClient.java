package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// RestClientProductClient — implements ProductClient using Spring's RestClient (Spring 6.1+)
// RestClient is the modern, fluent, synchronous HTTP client replacing RestTemplate.
// Conceptually identical to @FeignClient — both are declarative HTTP clients.
// The Resilience4j annotations on OrderService.getProduct() wrap calls to this bean.
@Component
public class RestClientProductClient implements ProductClient {

    private final RestClient restClient;

    public RestClientProductClient(@Value("${product-service.url}") String baseUrl) {
        // RestClient.builder() — fluent builder, sets base URL for all requests
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public ProductResponse getProductById(Long id) {
        // fluent API: method → uri → retrieve → body
        // throws RestClientException (subclass of RuntimeException) on HTTP errors
        return restClient.get()
                .uri("/api/products/{id}", id)
                .retrieve()
                .body(ProductResponse.class);
    }
}
