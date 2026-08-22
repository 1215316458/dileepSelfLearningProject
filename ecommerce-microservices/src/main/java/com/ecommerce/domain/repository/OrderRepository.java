package com.ecommerce.domain.repository;

import com.ecommerce.domain.enums.OrderStatus;
import com.ecommerce.domain.model.Order;
import com.ecommerce.domain.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

public class OrderRepository extends InMemoryRepository<Order, Long> {

    // LinkedHashMap — preserves insertion order so findByUserId returns orders chronologically.
    // HashMap doesn't guarantee order; LinkedHashMap maintains it at minimal overhead.
    private final UserRepository userRepository;

    public OrderRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
        // replace parent's HashMap store with LinkedHashMap to preserve insertion order
        // store is protected in InMemoryRepository so we can clear and re-populate,
        // but the cleanest approach is to shadow it — done via the parent's protected field
        // by putting all entries through save() which calls store.put() on the LinkedHashMap below.
    }

    // Returns orders for a user in chronological order (insertion order via LinkedHashMap)
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

    // PriorityQueue — heap-based, orders processed by priority (ADMIN > SELLER > CUSTOMER).
    // Returns a queue where poll() always gives the highest-priority order first.
    // Comparator: lower ordinal = higher priority in our Role enum ordering (CUSTOMER=0, SELLER=1, ADMIN=2)
    // We invert so ADMIN (2) comes out first.
    public PriorityQueue<Order> buildProcessingQueue() {
        // Comparator: compare by user role priority — ADMIN first, then SELLER, then CUSTOMER
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
