package com.ecommerce.product_service.seeder;

import com.ecommerce.product_service.domain.entity.Product;
import com.ecommerce.product_service.domain.enums.Category;
import com.ecommerce.product_service.repository.ProductRepository;
import com.ecommerce.product_service.repository.ProductSpecification;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder {

    private final ProductRepository productRepository;

    // constructor injection — no @Autowired needed (single constructor rule)
    public DataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostConstruct
    public void seed() {
        if (productRepository.count() > 0) return;  // skip if already seeded (e.g. during tests)
        // --- Seed 5 products ---
        productRepository.save(new Product("Laptop Pro",      "High-end laptop",    new BigDecimal("1200.00"), 10,  Category.ELECTRONICS));
        productRepository.save(new Product("Wireless Mouse",  "Ergonomic mouse",    new BigDecimal("45.00"),   50,  Category.ELECTRONICS));
        productRepository.save(new Product("Clean Code",      "Robert C. Martin",   new BigDecimal("35.00"),   30,  Category.BOOKS));
        productRepository.save(new Product("Running Shoes",   "Lightweight shoes",  new BigDecimal("89.99"),   0,   Category.SPORTS));   // out of stock
        productRepository.save(new Product("Java Cookbook",   "Recipes for Java",   new BigDecimal("29.99"),   20,  Category.BOOKS));

        System.out.println("\n========== DataSeeder Results ==========");

        // 1. findByCategory — derived query
        List<Product> electronics = productRepository.findByCategory(Category.ELECTRONICS);
        System.out.println("\n[1] Electronics: " + electronics);

        // 2. findByPriceBetween — derived query
        List<Product> midRange = productRepository.findByPriceBetween(new BigDecimal("30"), new BigDecimal("100"));
        System.out.println("\n[2] Price $30-$100: " + midRange);

        // 3. findCheapestInCategory — JPQL @Query
        productRepository.findCheapestInCategory(Category.BOOKS)
                .ifPresent(p -> System.out.println("\n[3] Cheapest Book: " + p));

        // 4. findOutOfStockProducts — native @Query
        List<Product> outOfStock = productRepository.findOutOfStockProducts();
        System.out.println("\n[4] Out of stock: " + outOfStock);

        // 5. Specification: hasCategory AND inStock — composed with .and()
        Specification<Product> spec = ProductSpecification.hasCategory(Category.ELECTRONICS)
                .and(ProductSpecification.inStock());
        List<Product> inStockElectronics = productRepository.findAll(spec);
        System.out.println("\n[5] Electronics in stock (Specification): " + inStockElectronics);

        // 6. Pagination: first page of ELECTRONICS, 2 per page, sorted by price asc
        Page<Product> page = productRepository.findByCategory(
                Category.ELECTRONICS,
                PageRequest.of(0, 2, Sort.by("price").ascending())
        );
        System.out.println("\n[6] Paginated Electronics (page 0, size 2):");
        System.out.println("    Total elements : " + page.getTotalElements());
        System.out.println("    Total pages    : " + page.getTotalPages());
        System.out.println("    Content        : " + page.getContent());

        // 7. findByCategoryAndStockQuantityGreaterThan — multi-condition derived query
        List<Product> booksInStock = productRepository.findByCategoryAndStockQuantityGreaterThan(Category.BOOKS, 0);
        System.out.println("\n[7] Books with stock > 0: " + booksInStock);

        System.out.println("\n========================================\n");
    }
}
