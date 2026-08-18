package com.ecommerce.domain.pattern.factory;

import com.ecommerce.domain.pattern.observer.*;

// Factory Pattern — centralizes object creation logic.
// Callers ask for a publisher by type; the factory decides which listeners to wire up.
// Benefit: adding a new publisher type only requires changing this one class.
public class EventPublisherFactory {

    public enum PublisherType {
        ORDER,      // email + sms + inventory
        PAYMENT,    // email + sms
        INVENTORY   // inventory only
    }

    // Static factory method — no need to instantiate the factory itself
    public static EventPublisher create(PublisherType type) {
        InMemoryEventPublisher publisher = new InMemoryEventPublisher();

        switch (type) {
            case ORDER:
                publisher.subscribe(EventType.ORDER_PLACED, new EmailNotificationListener());
                publisher.subscribe(EventType.ORDER_PLACED, new SmsNotificationListener());
                publisher.subscribe(EventType.ORDER_PLACED, new InventoryListener());
                publisher.subscribe(EventType.ORDER_CANCELLED, new EmailNotificationListener());
                publisher.subscribe(EventType.ORDER_SHIPPED, new SmsNotificationListener());
                break;

            case PAYMENT:
                publisher.subscribe(EventType.PAYMENT_PROCESSED, new EmailNotificationListener());
                publisher.subscribe(EventType.PAYMENT_PROCESSED, new SmsNotificationListener());
                publisher.subscribe(EventType.PAYMENT_FAILED, new EmailNotificationListener());
                publisher.subscribe(EventType.PAYMENT_FAILED, new SmsNotificationListener());
                break;

            case INVENTORY:
                publisher.subscribe(EventType.STOCK_LOW, new InventoryListener());
                break;

            default:
                throw new IllegalArgumentException("Unknown publisher type: " + type);
        }

        return publisher;
    }
}
