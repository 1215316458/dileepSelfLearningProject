package com.ecommerce.domain.repository.concurrency;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.enums.OrderStatus;
import com.ecommerce.domain.model.CartItem;
import com.ecommerce.domain.model.Order;
import com.ecommerce.domain.model.Product;

import java.math.BigDecimal;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderQueue {

    private static final int PRODUCER_COUNT = 5;
    private static final int CONSUMER_COUNT = 3;
    private static final int ORDERS_PER_PRODUCER = 4; // each producer places 4 orders = 20 total

    // LinkedBlockingQueue — put() blocks if full, take() blocks if empty
    // No busy-waiting: threads sleep until work is available
    // Capacity 50 — producers slow down if consumers fall behind (backpressure)
    private final LinkedBlockingQueue<Order> queue = new LinkedBlockingQueue<>(50);

    // Poison pill — sentinel value that signals a consumer to stop
    // Using a real Order object avoids null checks and is idiomatic Java
    private static final Order POISON_PILL = new Order.Builder()
            .id(-1L).userId(-1L)
            .addItem(new CartItem(
                    new Product(-1L, "POISON", "", BigDecimal.ONE, 1, Category.FOOD), 1))
            .build();

    // volatile — guarantees all threads see the latest value immediately
    // Without volatile, a thread may read a CPU-cached stale value of `running`
    // Note: volatile guarantees VISIBILITY, not atomicity — use AtomicBoolean for CAS operations
    private volatile boolean running = true;

    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger producedCount  = new AtomicInteger(0);

    public void start() throws InterruptedException {
        Thread[] producers = new Thread[PRODUCER_COUNT];
        Thread[] consumers = new Thread[CONSUMER_COUNT];

        // start consumers first — ready to consume before producers start
        for (int i = 0; i < CONSUMER_COUNT; i++) {
            final int consumerId = i + 1;
            consumers[i] = new Thread(() -> consume(consumerId), "Consumer-" + consumerId);
            consumers[i].start();
        }

        // start producers
        for (int i = 0; i < PRODUCER_COUNT; i++) {
            final int producerId = i + 1;
            producers[i] = new Thread(() -> produce(producerId), "Producer-" + producerId);
            producers[i].start();
        }

        // wait for all producers to finish
        for (Thread p : producers) p.join();

        // send one poison pill — each consumer that receives it re-queues it for the next consumer
        // so all CONSUMER_COUNT consumers will eventually receive and stop
        queue.put(POISON_PILL);

        // wait for all consumers to finish
        for (Thread c : consumers) c.join();

        System.out.println("\n  Total produced:  " + producedCount.get());
        System.out.println("  Total processed: " + processedCount.get());
        System.out.println("  All orders processed exactly once: "
                + (producedCount.get() == processedCount.get()));
    }

    private void produce(int producerId) {
        for (int i = 0; i < ORDERS_PER_PRODUCER; i++) {
            try {
                Order order = buildOrder((long) (producerId * 100 + i), (long) producerId);
                queue.put(order); // blocks if queue is full
                int total = producedCount.incrementAndGet();
                System.out.println("  [" + Thread.currentThread().getName() + "] produced order "
                        + order.getIdentity() + " (total produced: " + total + ")");
                Thread.sleep(10); // simulate work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void consume(int consumerId) {
        while (running) {
            try {
                Order order = queue.take(); // blocks if queue is empty — no busy-waiting

                // poison pill check — re-queue for the next consumer, then stop
                if (order == POISON_PILL) {
                    queue.put(POISON_PILL);
                    break;
                }

                // simulate order processing
                Thread.sleep(20);
                int total = processedCount.incrementAndGet();
                System.out.println("  [" + Thread.currentThread().getName() + "] processed order "
                        + order.getIdentity() + " (total processed: " + total + ")");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private Order buildOrder(Long orderId, Long userId) {
        Product product = new Product(1L, "Laptop", "Test", new BigDecimal("999.00"), 100, Category.ELECTRONICS);
        return new Order.Builder()
                .id(orderId)
                .userId(userId)
                .addItem(new CartItem(product, 1))
                .status(OrderStatus.PENDING)
                .shippingAddress("Address-" + userId)
                .build();
    }
}
