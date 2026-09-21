package com.ecommerce.product_service.dto;

import com.ecommerce.product_service.domain.entity.Product;
import com.ecommerce.product_service.domain.enums.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private Category category;
    private boolean inStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // static factory — converts entity to DTO
    public static ProductResponse from(Product product) {
        ProductResponse dto = new ProductResponse();
        dto.id            = product.getId();
        dto.name          = product.getName();
        dto.description   = product.getDescription();
        dto.price         = product.getPrice();
        dto.stockQuantity = product.getStockQuantity();
        dto.category      = product.getCategory();
        dto.inStock       = product.getStockQuantity() > 0;
        dto.createdAt     = product.getCreatedAt();
        dto.updatedAt     = product.getUpdatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
    public Category getCategory() { return category; }
    public boolean isInStock() { return inStock; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
