package com.ecommerce.domain.repository;

import com.ecommerce.domain.enums.OrderStatus;
import com.ecommerce.domain.model.Order;
import com.ecommerce.domain.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

public class OrderRepository extends InMemoryRepository<Order, Long> {

    private final UserRepository userRepository;

    public OrderRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Returns orders for a user in chronological order (insertion order via ConcurrentHashMap)
    public List<Order> findByUserId(Long userId) {
        List<Order> result = new ArrayList<>();
        for (Order o : store.values()) {
            if (o.getUserId().equals(userId)) result.add(o);
        }
        return Collections.unmodifiableList(result);
    }

    public List<Order> findByStatus(OrderStatus status) {
        List<Order> result = new ArrayList<>();
        for (Order o : store.values()) {
            if (o.getStatus() == status) result.add(o);
        }
        return Collections.unmodifiableList(result);
    }

    // Orders placed between from and to (inclusive) — useful for monthly reports
    public List<Order> findByDateRange(LocalDateTime from, LocalDateTime to) {
        List<Order> result = new ArrayList<>();
        for (Order o : store.values()) {
            LocalDateTime created = o.getCreatedAt();
            if (!created.isBefore(from) && !created.isAfter(to)) {
                result.add(o);
            }
        }
        return Collections.unmodifiableList(result);
    }

    // Total amount spent by a user across all their orders
    public BigDecimal getTotalSpendByUser(Long userId) {
        return store.values().stream()
                .filter(o -> o.getUserId().equals(userId))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // PriorityQueue — heap-based, orders processed by priority (ADMIN > SELLER > CUSTOMER)
    // poll() always returns the highest-priority order first
    public PriorityQueue<Order> buildProcessingQueue() {
        PriorityQueue<Order> queue = new PriorityQueue<>((o1, o2) -> {
            int p1 = getRolePriority(o1.getUserId());
            int p2 = getRolePriority(o2.getUserId());
            return Integer.compare(p2, p1); // descending — higher priority first
        });
        // only enqueue PENDING orders — those are waiting to be processed
        for (Order o : store.values()) {
            if (o.getStatus() == OrderStatus.PENDING) queue.offer(o);
        }
        return queue;
    }

    // ADMIN=2, SELLER=1, CUSTOMER=0 — higher number = higher priority in queue
    private int getRolePriority(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) return 0;
        return switch (user.get().getRole()) {
            case ADMIN    -> 2;
            case SELLER   -> 1;
            case CUSTOMER -> 0;
        };
    }
}
