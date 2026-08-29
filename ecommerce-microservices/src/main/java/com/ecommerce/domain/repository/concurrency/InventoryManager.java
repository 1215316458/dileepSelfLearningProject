package com.ecommerce.domain.repository.concurrency;

import com.ecommerce.domain.exception.InsufficientStockException;
import com.ecommerce.domain.model.Product;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InventoryManager {

    // ReentrantReadWriteLock — two locks in one:
    // readLock:  many threads can hold it simultaneously (reads don't conflict)
    // writeLock: only ONE thread can hold it, blocks ALL readers and other writers
    // Better than synchronized: synchronized blocks everyone even for reads
    private final ReentrantReadWriteLock lock     = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock  readLock  = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    // productId -> current stock level
    // HashMap is fine here — the ReadWriteLock protects all access to it
    private final Map<Long, Integer> stockMap = new HashMap<>();

    public void addProduct(Product product) {
        writeLock.lock();
        try {
            stockMap.put(product.getIdentity(), product.getStock());
        } finally {
            // always release in finally — prevents lock leak if an exception is thrown
            writeLock.unlock();
        }
    }

    // readLock — 100 threads can call this simultaneously, no blocking between them
    public int getStock(Long productId) {
        readLock.lock();
        try {
            return stockMap.getOrDefault(productId, 0);
        } finally {
            readLock.unlock();
        }
    }

    public boolean checkAvailability(Long productId, int quantity) {
        readLock.lock();
        try {
            return stockMap.getOrDefault(productId, 0) >= quantity;
        } finally {
            readLock.unlock();
        }
    }

    // writeLock — only ONE thread executes this at a time, all readers are blocked until done
    public void reserveStock(Long productId, int quantity) {
        writeLock.lock();
        try {
            int current = stockMap.getOrDefault(productId, 0);
            if (current < quantity) {
                throw new InsufficientStockException(productId, quantity, current);
            }
            stockMap.put(productId, current - quantity);
        } finally {
            writeLock.unlock();
        }
    }

    public void releaseStock(Long productId, int quantity) {
        writeLock.lock();
        try {
            // merge: if key exists add quantity, otherwise insert quantity
            stockMap.merge(productId, quantity, Integer::sum);
        } finally {
            writeLock.unlock();
        }
    }
}
