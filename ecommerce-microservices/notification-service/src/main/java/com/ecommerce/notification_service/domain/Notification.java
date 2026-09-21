package com.ecommerce.notification_service.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document — no schema, no migrations, no foreign keys.
 * Each notification is a self-contained record; we never JOIN across collections.
 *
 * @Document maps to a MongoDB collection (like @Entity maps to a SQL table).
 * @Id maps to MongoDB's _id field (stored as ObjectId by default when String).
 * @Indexed creates a single-field index on userId for fast per-user queries.
 * @CreatedDate requires @EnableMongoAuditing on the application class.
 */
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private NotificationType type;

    private String title;
    private String message;

    private boolean read = false;

    @CreatedDate
    private Instant createdAt;

    // Reference to the source domain object (orderId, productId, etc.)
    private Long referenceId;

    protected Notification() {}

    public Notification(Long userId, NotificationType type, String title, String message, Long referenceId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.referenceId = referenceId;
    }

    public void markRead() {
        this.read = true;
    }

    public String getId() { return id; }
    public Long getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getReferenceId() { return referenceId; }
}
