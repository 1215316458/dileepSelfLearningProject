package com.ecommerce.order_service.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

// OrderItem is the "many" side — many items belong to one order
// No separate repository needed — managed entirely through Order (cascade)
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 200)
    private String productName;   // snapshot at order time — product name may change later

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;  // snapshot at order time — price may change later

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;   // unitPrice * quantity, stored for query efficiency

    // LAZY — don't load the whole Order when we only need the item
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    protected OrderItem() {}

    public OrderItem(Long productId, String productName, BigDecimal unitPrice, Integer quantity, Order order) {
        this.productId   = productId;
        this.productName = productName;
        this.unitPrice   = unitPrice;
        this.quantity    = quantity;
        this.subtotal    = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.order       = order;
    }

    public Long getId()              { return id; }
    public Long getProductId()       { return productId; }
    public String getProductName()   { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getQuantity()     { return quantity; }
    public BigDecimal getSubtotal()  { return subtotal; }
    public Order getOrder()          { return order; }
}
