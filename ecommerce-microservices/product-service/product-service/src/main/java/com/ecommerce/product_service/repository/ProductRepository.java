package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.domain.entity.Product;
import com.ecommerce.product_service.domain.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // --- Derived Queries (Spring generates SQL from method name) ---

    List<Product> findByCategory(Category category);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);

    // combines two conditions: category AND stock > given value
    List<Product> findByCategoryAndStockQuantityGreaterThan(Category category, int stock);

    Optional<Product> findByName(String name);

    // --- JPQL Query (DB-agnostic, uses entity/field names not table/column names) ---

    @Query("SELECT p FROM Product p WHERE p.price = (SELECT MIN(p2.price) FROM Product p2 WHERE p2.category = :category)")
    Optional<Product> findCheapestInCategory(@Param("category") Category category);

    // --- Native Query (raw SQL, DB-specific) ---

    @Query(value = "SELECT * FROM products WHERE stock_quantity = 0", nativeQuery = true)
    List<Product> findOutOfStockProducts();

    // --- Pagination (Spring handles LIMIT/OFFSET) ---

    Page<Product> findByCategory(Category category, Pageable pageable);
}
