package com.ecommerce.domain.concurrency;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class FlashSaleManager {

    private final int totalStock;
    private final int maxConcurrentBuyers;
    private final int batchSize;

    // AtomicInteger — lock-free stock decrement via CAS (Compare-And-Set)
    // compareAndSet(expected, update): only updates if current value == expected
    // If two threads read stock=5 simultaneously, only ONE will succeed the CAS — the other retries
    private final AtomicInteger remainingStock;

    // CountDownLatch — one-time gate: sale starts only after all services initialise
    // await() blocks until count reaches 0; countDown() decrements the count
    // Cannot be reset — use CyclicBarrier if you need reuse
    private final CountDownLatch startGate;

    // Semaphore — limits concurrent access to a resource (like a bouncer at a club)
    // acquire() blocks if no permits available; release() returns a permit
    // Here: max 100 buyers can be in the "checkout" section simultaneously
    private final Semaphore buyerSlots;

    // CyclicBarrier — reusable synchronisation point
    // All threads wait at barrier until batchSize threads have arrived, then all proceed together
    // Unlike CountDownLatch, CyclicBarrier resets automatically after each batch
    private final CyclicBarrier batchBarrier;

    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failCount    = new AtomicInteger(0);
    private final AtomicInteger batchCount   = new AtomicInteger(0);

    public FlashSaleManager(int totalStock, int maxConcurrentBuyers, int batchSize) {
        this.totalStock          = totalStock;
        this.maxConcurrentBuyers = maxConcurrentBuyers;
        this.batchSize           = batchSize;
        this.remainingStock      = new AtomicInteger(totalStock);
        this.startGate           = new CountDownLatch(3); // 3 services must init before sale starts
        this.buyerSlots          = new Semaphore(maxConcurrentBuyers);
        this.batchBarrier        = new CyclicBarrier(batchSize, () ->
                System.out.println("  [Batch " + batchCount.incrementAndGet() + "] processed " + batchSize + " buyers"));
    }

    // Called by each service to signal it's ready — sale starts when all 3 are ready
    public void serviceReady() {
        startGate.countDown();
    }

    public void attemptPurchase(long buyerId) {
        try {
            startGate.await(); // block until all 3 services are ready

            buyerSlots.acquire(); // block if 100 buyers already in checkout
            try {
                boolean purchased = tryDecrementStock();
                if (purchased) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }

                // wait at barrier until batchSize buyers have attempted — then process as a batch
                try {
                    batchBarrier.await();
                } catch (BrokenBarrierException e) {
                    // barrier broken — another thread threw an exception, safe to ignore here
                }

            } finally {
                buyerSlots.release(); // always release permit — even if purchase failed
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // CAS loop — lock-free stock decrement
    // Read current → compute new → swap only if current hasn't changed since we read it
    // If another thread changed it between our read and swap, retry
    private boolean tryDecrementStock() {
        while (true) {
            int current = remainingStock.get();
            if (current <= 0) return false;                          // sold out
            if (remainingStock.compareAndSet(current, current - 1)) return true; // CAS succeeded
            // CAS failed — another thread changed the value, retry the loop
        }
    }

    public int getSuccessCount()   { return successCount.get(); }
    public int getFailCount()      { return failCount.get(); }
    public int getRemainingStock() { return remainingStock.get(); }
}
