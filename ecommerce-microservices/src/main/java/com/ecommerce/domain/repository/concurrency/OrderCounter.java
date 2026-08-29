package com.ecommerce.domain.repository.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

public class OrderCounter {

    // AtomicInteger — uses CAS (Compare-And-Set) hardware instruction
    // No lock needed: thread reads value, computes new value, swaps only if value hasn't changed
    // Much faster than synchronized for simple counters — no thread blocking
    private final AtomicInteger count = new AtomicInteger(0);

    // incrementAndGet — atomically increments and returns the NEW value
    // Two threads calling this simultaneously will always get different values
    public int increment() {
        return count.incrementAndGet();
    }

    public int decrement() {
        return count.decrementAndGet();
    }

    public int get() {
        return count.get();
    }

    public void reset() {
        count.set(0);
    }
}
