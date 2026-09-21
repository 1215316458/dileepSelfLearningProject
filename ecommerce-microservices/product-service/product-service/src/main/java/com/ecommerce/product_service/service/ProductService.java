package com.ecommerce.product_service.service;

import com.ecommerce.product_service.domain.entity.Product;
import com.ecommerce.product_service.domain.enums.Category;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.exception.DuplicateProductException;
import com.ecommerce.product_service.exception.ProductNotFoundException;
import com.ecommerce.product_service.repository.ProductRepository;
import com.ecommerce.product_service.repository.ProductSpecification;
import jakarta.annotation.PostConstruct;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)  // all methods read-only by default — overridden for writes
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // --- READ operations (inherit readOnly = true from class level) ---

    @Cacheable(value = "products", key = "#id")  // cache result by id
    public ProductResponse findById(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductResponse::from);
    }

    public List<ProductResponse> findByCategory(Category category) {
        return productRepository.findByCategory(category)
                .stream().map(ProductResponse::from).toList();
    }

    public Page<ProductResponse> search(Category category, BigDecimal minPrice,
                                        BigDecimal maxPrice, Boolean inStock,
                                        String keyword, Pageable pageable) {
        Specification<Product> spec = Specification.where((Specification<Product>) null);
        if (category != null)  spec = spec.and(ProductSpecification.hasCategory(category));
        if (minPrice != null && maxPrice != null) spec = spec.and(ProductSpecification.hasPriceBetween(minPrice, maxPrice));
        if (Boolean.TRUE.equals(inStock))         spec = spec.and(ProductSpecification.inStock());
        if (keyword != null && !keyword.isBlank()) spec = spec.and(ProductSpecification.nameContains(keyword));
        return productRepository.findAll(spec, pageable).map(ProductResponse::from);
    }

    // --- WRITE operations (override class-level readOnly = true) ---

    @Transactional
    @CacheEvict(value = "products", allEntries = true)  // clear all cached products on write
    public ProductResponse create(ProductRequest request) {
        productRepository.findByName(request.getName()).ifPresent(p -> {
            throw new DuplicateProductException(request.getName());
        });
        Product product = new Product(
                request.getName(), request.getDescription(),
                request.getPrice(), request.getStockQuantity(), request.getCategory()
        );
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")  // evict only this product from cache
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void delete(Long id) {
        if (!productRepository.existsById(id)) throw new ProductNotFoundException(id);
        productRepository.deleteById(id);
    }


}
