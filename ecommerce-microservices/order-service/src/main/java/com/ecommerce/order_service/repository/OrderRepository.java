package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.domain.entity.Order;
import com.ecommerce.order_service.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(OrderStatus status);

    // @EntityGraph — fetch items in one JOIN query, avoiding N+1
    @EntityGraph(attributePaths = "items")
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    @EntityGraph(attributePaths = "items")
    List<Order> findByUserId(Long userId, org.springframework.data.domain.Sort sort);

    // find stuck orders — PENDING for more than a given duration (used by @Scheduled)
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.createdAt < :cutoff")
    List<Order> findStuckOrders(@Param("cutoff") Instant cutoff);
}
