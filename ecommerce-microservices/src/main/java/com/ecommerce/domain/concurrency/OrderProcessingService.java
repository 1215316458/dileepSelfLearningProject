package com.ecommerce.domain.concurrency;

import com.ecommerce.domain.exception.PaymentFailedException;
import com.ecommerce.domain.model.Order;
import com.ecommerce.domain.repository.concurrency.InventoryManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderProcessingService {

    // Named thread pool — easier to identify threads in logs/stack traces
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> new Thread(r, "order-processor-" + System.nanoTime())
    );

    private final InventoryManager inventoryManager;

    public OrderProcessingService(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
    }

    // Full async pipeline: validate → reserveInventory → processPayment → confirm → notify
    // thenApplyAsync — runs next stage on executor thread (non-blocking)
    // exceptionally — compensation: if payment fails, release stock and cancel order
    public CompletableFuture<String> processOrder(Order order) {
        return CompletableFuture
                .supplyAsync(() -> validate(order), executor)
                .thenApplyAsync(o -> reserveInventory(o), executor)
                .thenApplyAsync(o -> processPayment(o), executor)
                .thenApplyAsync(o -> confirmOrder(o), executor)
                .thenApplyAsync(o -> sendConfirmation(o), executor)
                .exceptionally(ex -> {
                    // compensation — release stock if payment failed mid-pipeline
                    releaseInventoryOnFailure(order);
                    return "FAILED: " + ex.getCause().getMessage();
                });
    }

    // allOf — submit N orders in parallel, wait for ALL to complete
    // Returns a future that completes when every order is done
    public CompletableFuture<Void> processBatch(List<Order> orders) {
        CompletableFuture<?>[] futures = orders.stream()
                .map(this::processOrder)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    // anyOf — race two payment providers, use whichever responds first
    // Returns a future that completes as soon as ONE provider responds
    public CompletableFuture<Object> racePaymentProviders(Order order) {
        CompletableFuture<String> provider1 = CompletableFuture.supplyAsync(() -> {
            simulateDelay(80);
            return "Provider1: payment accepted for order " + order.getIdentity();
        }, executor);

        CompletableFuture<String> provider2 = CompletableFuture.supplyAsync(() -> {
            simulateDelay(120);
            return "Provider2: payment accepted for order " + order.getIdentity();
        }, executor);

        // anyOf — whichever finishes first wins, the other is ignored
        return CompletableFuture.anyOf(provider1, provider2);
    }

    public void shutdown() {
        executor.shutdown();
    }

    // --- pipeline stages ---

    private Order validate(Order order) {
        simulateDelay(10);
        if (order.getTotalAmount() == null) throw new IllegalArgumentException("Invalid order: no total");
        return order;
    }

    private Order reserveInventory(Order order) {
        simulateDelay(15);
        order.getItems().forEach(item ->
                inventoryManager.reserveStock(item.getProduct().getIdentity(), item.getQuantity())
        );
        return order;
    }

    private Order processPayment(Order order) {
        simulateDelay(30);
        // simulate 10% payment failure rate for demo
        if (order.getIdentity() != null && order.getIdentity() % 10 == 0) {
            throw new PaymentFailedException("card declined for order " + order.getIdentity());
        }
        return order;
    }

    private Order confirmOrder(Order order) {
        simulateDelay(10);
        return order;
    }

    private String sendConfirmation(Order order) {
        simulateDelay(5);
        return "SUCCESS: order " + order.getIdentity() + " confirmed for userId=" + order.getUserId();
    }

    private void releaseInventoryOnFailure(Order order) {
        order.getItems().forEach(item ->
                inventoryManager.releaseStock(item.getProduct().getIdentity(), item.getQuantity())
        );
    }

    private void simulateDelay(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
