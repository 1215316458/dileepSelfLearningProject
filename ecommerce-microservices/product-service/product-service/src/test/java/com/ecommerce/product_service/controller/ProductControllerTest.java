package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.domain.entity.Product;
import com.ecommerce.product_service.domain.enums.Category;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.exception.GlobalExceptionHandler;
import com.ecommerce.product_service.exception.ProductNotFoundException;
import com.ecommerce.product_service.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    ProductService productService;

    @InjectMocks
    ProductController productController;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // build MockMvc manually with the GlobalExceptionHandler so error responses work
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getById_existingProduct_returns200() throws Exception {
        ProductResponse response = buildResponse("Laptop");
        when(productService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Laptop"));
    }

    @Test
    void getById_missingProduct_returns404() throws Exception {
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        ProductRequest request  = buildRequest("Laptop", Category.ELECTRONICS);
        ProductResponse response = buildResponse("Laptop");
        when(productService.create(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Laptop"));
    }

    @Test
    void create_blankName_returns400WithFieldError() throws Exception {
        ProductRequest request = buildRequest("", Category.ELECTRONICS);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void delete_existingProduct_returns200() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product deleted successfully"));
    }

    private ProductRequest buildRequest(String name, Category category) {
        ProductRequest req = new ProductRequest();
        req.setName(name);
        req.setDescription("desc");
        req.setPrice(new BigDecimal("100.00"));
        req.setStockQuantity(10);
        req.setCategory(category);
        return req;
    }

    private ProductResponse buildResponse(String name) {
        Product product = new Product(name, "desc", new BigDecimal("100.00"), 10, Category.ELECTRONICS);
        return ProductResponse.from(product);
    }
}
