package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.domain.entity.Product;
import com.ecommerce.product_service.domain.enums.Category;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecification {

    // each method returns a lambda: (root, query, criteriaBuilder) -> Predicate
    // root     = the FROM clause (Product table)
    // query    = the full query object (SELECT ... FROM ... WHERE)
    // cb       = factory for building conditions (equal, like, between, etc.)

    public static Specification<Product> hasCategory(Category category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Product> hasPriceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> cb.between(root.get("price"), min, max);
    }

    public static Specification<Product> nameContains(String keyword) {
        // % wildcard on both sides for contains, toLowerCase for case-insensitive
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("stockQuantity"), 0);
    }
}
