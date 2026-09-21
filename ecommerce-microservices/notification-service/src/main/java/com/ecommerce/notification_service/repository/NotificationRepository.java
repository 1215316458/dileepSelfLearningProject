package com.ecommerce.notification_service.repository;

import com.ecommerce.notification_service.domain.Notification;
import com.ecommerce.notification_service.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * MongoRepository<T, ID> — same interface contract as JpaRepository but backed by MongoDB.
 * Spring Data generates the implementation at runtime from method name conventions,
 * exactly like JPA derived queries.
 *
 * Key difference from JPA:
 *  - No @Transactional by default (MongoDB multi-document transactions exist but are rare)
 *  - No lazy loading / N+1 problem — documents are self-contained
 *  - Queries use MongoDB query language, not JPQL
 */
public interface NotificationRepository extends MongoRepository<Notification, String> {

    // Derived query: Spring Data translates method name → MongoDB filter {userId: ?0}
    Page<Notification> findByUserId(Long userId, Pageable pageable);

    // Derived query with multiple conditions
    List<Notification> findByUserIdAndReadFalse(Long userId);

    // Count unread for badge display
    long countByUserIdAndReadFalse(Long userId);

    // @Query uses MongoDB JSON query syntax (not JPQL)
    @Query("{ 'userId': ?0, 'type': ?1 }")
    List<Notification> findByUserIdAndType(Long userId, NotificationType type);

    // Delete all notifications for a user (GDPR right to erasure)
    void deleteByUserId(Long userId);
}
