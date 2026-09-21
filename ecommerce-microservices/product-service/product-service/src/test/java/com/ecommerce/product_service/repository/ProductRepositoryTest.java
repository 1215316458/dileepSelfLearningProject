package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.domain.entity.Product;
import com.ecommerce.product_service.domain.enums.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional  // rolls back after each test — keeps DB clean
class ProductRepositoryTest {

    @Autowired
    ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        productRepository.save(new Product("Laptop",        "desc", new BigDecimal("1200.00"), 10, Category.ELECTRONICS));
        productRepository.save(new Product("Mouse",         "desc", new BigDecimal("45.00"),    5, Category.ELECTRONICS));
        productRepository.save(new Product("Clean Code",    "desc", new BigDecimal("35.00"),   20, Category.BOOKS));
        productRepository.save(new Product("Running Shoes", "desc", new BigDecimal("89.99"),    0, Category.SPORTS));
    }

    @Test
    void findByCategory_returnsMatchingProducts() {
        List<Product> electronics = productRepository.findByCategory(Category.ELECTRONICS);
        assertThat(electronics).hasSize(2);
        assertThat(electronics).allMatch(p -> p.getCategory() == Category.ELECTRONICS);
    }

    @Test
    void findByPriceBetween_returnsProductsInRange() {
        List<Product> result = productRepository.findByPriceBetween(
                new BigDecimal("30"), new BigDecimal("100")
        );
        assertThat(result).hasSize(3);
    }

    @Test
    void findCheapestInCategory_returnsLowestPriced() {
        Optional<Product> cheapest = productRepository.findCheapestInCategory(Category.ELECTRONICS);
        assertThat(cheapest).isPresent();
        assertThat(cheapest.get().getName()).isEqualTo("Mouse");
    }

    @Test
    void findOutOfStockProducts_returnsZeroStockOnly() {
        List<Product> outOfStock = productRepository.findOutOfStockProducts();
        assertThat(outOfStock).hasSize(1);
        assertThat(outOfStock.get(0).getName()).isEqualTo("Running Shoes");
    }

    @Test
    void specification_categoryAndInStock_filtersCorrectly() {
        Specification<Product> spec = ProductSpecification.hasCategory(Category.ELECTRONICS)
                .and(ProductSpecification.inStock());
        List<Product> result = productRepository.findAll(spec);
        assertThat(result).hasSize(2);
    }

    @Test
    void findByCategory_withPageable_returnsPaginatedResults() {
        Page<Product> page = productRepository.findByCategory(
                Category.ELECTRONICS, PageRequest.of(0, 1)
        );
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findByName_existingName_returnsProduct() {
        Optional<Product> product = productRepository.findByName("Laptop");
        assertThat(product).isPresent();
    }

    @Test
    void findByName_missingName_returnsEmpty() {
        Optional<Product> product = productRepository.findByName("NonExistent");
        assertThat(product).isEmpty();
    }
}
