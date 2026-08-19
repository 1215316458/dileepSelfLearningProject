package com.ecommerce.domain.repository;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class ProductRepository extends InMemoryRepository<Product, Long> {

    // Price index — TreeMap keeps prices sorted, enabling O(log n) range queries via subMap().
    // Map<price, List<Product>> because multiple products can share the same price.
    // HashMap would require scanning all entries for range queries — O(n).
    private final TreeMap<BigDecimal, List<Product>> priceIndex = new TreeMap<>();

    @Override
    public Product save(Product product) {
        // maintain price index in sync with the main store
        priceIndex
            .computeIfAbsent(product.getPrice(), k -> new ArrayList<>())
            .add(product);
        return super.save(product);
    }

    @Override
    public void deleteById(Long id) {
        // remove from price index before removing from store
        findById(id).ifPresent(p -> {
            List<Product> bucket = priceIndex.get(p.getPrice());
            if (bucket != null) {
                bucket.remove(p);
                if (bucket.isEmpty()) priceIndex.remove(p.getPrice());
            }
        });
        super.deleteById(id);
    }

    // O(n) scan — category is not indexed, acceptable for in-memory store
    public List<Product> findByCategory(Category category) {
        List<Product> result = new ArrayList<>();
        for (Product p : store.values()) {
            if (p.getCategory() == category) result.add(p);
        }
        return Collections.unmodifiableList(result);
    }

    // O(log n) range query — TreeMap.subMap() returns a view of entries between min and max
    // inclusive on both ends (true, true)
    public List<Product> findByPriceRange(BigDecimal min, BigDecimal max) {
        List<Product> result = new ArrayList<>();
        // subMap(min, inclusive, max, inclusive) — only iterates the relevant slice of the tree
        for (List<Product> bucket : priceIndex.subMap(min, true, max, true).values()) {
            result.addAll(bucket);
        }
        return Collections.unmodifiableList(result);
    }

    // Case-insensitive name search — O(n), no index needed for simple contains check
    public List<Product> findByNameContaining(String keyword) {
        String lower = keyword.toLowerCase();
        List<Product> result = new ArrayList<>();
        for (Product p : store.values()) {
            if (p.getName().toLowerCase().contains(lower)) result.add(p);
        }
        return Collections.unmodifiableList(result);
    }
}
