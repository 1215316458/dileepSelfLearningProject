package com.ecommerce.order_service.domain.entity;

import com.ecommerce.order_service.domain.enums.OrderStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user_id", columnList = "userId"),
    @Index(name = "idx_order_status",  columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;   // reference to user-service — no FK across services (microservice rule)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String shippingAddress;

    // cascade = ALL — saving/deleting Order also saves/deletes its items
    // orphanRemoval — removing item from list deletes it from DB
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    protected Order() {}

    public Order(Long userId, String shippingAddress) {
        this.userId          = userId;
        this.shippingAddress = shippingAddress;
    }

    // helper — keeps bidirectional relationship consistent and recalculates total
    public void addItem(OrderItem item) {
        items.add(item);
        totalAmount = totalAmount.add(item.getSubtotal());
    }

    public Long getId()                  { return id; }
    public Long getUserId()              { return userId; }
    public OrderStatus getStatus()       { return status; }
    public BigDecimal getTotalAmount()   { return totalAmount; }
    public String getShippingAddress()   { return shippingAddress; }
    public List<OrderItem> getItems()    { return Collections.unmodifiableList(items); }
    public Instant getCreatedAt()        { return createdAt; }
    public Instant getUpdatedAt()        { return updatedAt; }

    public void setStatus(OrderStatus status)             { this.status = status; }
    public void setShippingAddress(String shippingAddress){ this.shippingAddress = shippingAddress; }
}
