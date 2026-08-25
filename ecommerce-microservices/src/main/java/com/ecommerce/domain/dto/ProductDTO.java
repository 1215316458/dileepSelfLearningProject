package com.ecommerce.domain.dto;

import com.ecommerce.domain.enums.Category;

import java.math.BigDecimal;

public class ProductDTO {

    private final Long id;
    private final String name;
    private final BigDecimal price;
    private final String description;
    private final int stock;
    private final Category category;

    public ProductDTO(Long id, String name, BigDecimal price, String description, int stock, Category category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.stock = stock;
        this.category = category;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getDescription() { return description; }
    public int getStock() { return stock; }
    public Category getCategory() { return category; }

    @Override
    public String toString() {
        return new StringBuilder("ProductDTO{")
                .append("id=").append(id)
                .append(", name=").append(name)
                .append(", price=$").append(price)
                .append(", stock=").append(stock)
                .append(", category=").append(category)
                .append('}')
                .toString();
    }
}
