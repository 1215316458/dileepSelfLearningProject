package com.ecommerce.domain.pattern.observer;

// Interface — defines the contract for any publisher implementation.
// default method — unsubscribeAll() is optional to override.
// If we add it as abstract, all existing implementations break.
// default lets us add new methods to interfaces without breaking anything.
public interface EventPublisher {

    void subscribe(EventType eventType, EventListener listener);

    void publish(EventType eventType, String eventData);

    default void unsubscribeAll(EventType eventType) {
        // implementations can override this if needed
    }
}
