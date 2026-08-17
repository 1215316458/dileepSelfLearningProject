package com.ecommerce.domain.model;

import com.ecommerce.domain.enums.Category;

import java.math.BigDecimal;
import java.util.Comparator;

public class Product extends BaseEntity<Long> {

    private String name;
    private String description;
    // BigDecimal — never use double/float for money (precision loss)
    private BigDecimal price;
    private int stock;
    private Category category;

    public Product(Long id, String name, String description, BigDecimal price, int stock, Category category) {
        setIdentity(id);
        setCreatedAt(java.time.LocalDateTime.now());
        setUpdatedAt(java.time.LocalDateTime.now());
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }

    // Static Comparators — reusable, no need to create new instances every time
    public static final Comparator<Product> BY_PRICE = Comparator.comparing(Product::getPrice);
    public static final Comparator<Product> BY_STOCK = Comparator.comparingInt(Product::getStock);
    public static final Comparator<Product> BY_CREATED_DATE = Comparator.comparing(Product::getCreatedAt);

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    @Override
    public String toString() {
        return new StringBuilder("Product{")
                .append("id=").append(getIdentity())
                .append(", name=").append(name)
                .append(", price=").append(price)
                .append(", stock=").append(stock)
                .append(", category=").append(category)
                .append('}')
                .toString();
    }
}
