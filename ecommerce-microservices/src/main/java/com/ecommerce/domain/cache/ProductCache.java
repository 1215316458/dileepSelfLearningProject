package com.ecommerce.domain.cache;

import java.util.WeakHashMap;

import com.ecommerce.domain.model.Product;

public class ProductCache {
    // NOT static — each ProductCache instance has its own independent map
    private final WeakHashMap<Long, Product> productCache = new WeakHashMap<>();
    
    public void put(Long productId, Product product) {
        productCache.put(productId, product);
    }
    
    public Product get(Long productId) {
        return productCache.get(productId);
    }
    
    public void remove(Long productId) {
        productCache.remove(productId);
    }

    public int size() {
        return productCache.size();
    }
}
