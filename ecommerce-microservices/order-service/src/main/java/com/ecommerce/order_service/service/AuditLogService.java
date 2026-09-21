package com.ecommerce.order_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// AuditLogService — demonstrates REQUIRES_NEW propagation
// Runs in its own independent transaction so audit entries survive
// even if the calling transaction rolls back
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    // REQUIRES_NEW — suspends the caller's transaction and starts a fresh one
    // This transaction commits independently of the outer transaction
    // Use case: audit logs must be persisted even when the business transaction fails
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrderCreated(Long orderId, Long userId) {
        // In production: persist to an audit_log table
        log.info("[AUDIT] ORDER_CREATED orderId={} userId={} at={}", orderId, userId,
                java.time.Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrderCancelled(Long orderId, Long userId) {
        log.info("[AUDIT] ORDER_CANCELLED orderId={} userId={} at={}", orderId, userId,
                java.time.Instant.now());
    }
}
