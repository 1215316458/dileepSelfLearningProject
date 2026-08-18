package com.ecommerce.domain.pattern.observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryEventPublisher implements EventPublisher {

    // Map<EventType, List<EventListener>>
    // Key   — the event type (ORDER_PLACED, PAYMENT_FAILED, etc.)
    // Value — list of all listeners interested in that event type
    // One event type can have MANY listeners (email + sms + inventory all listen to ORDER_PLACED)
    private final Map<EventType, List<EventListener>> listeners = new HashMap<>();

    @Override
    public void subscribe(EventType eventType, EventListener listener) {
        // computeIfAbsent — if no list exists for this eventType yet, create one.
        // Avoids manual null check: if(listeners.get(key) == null) listeners.put(key, new ArrayList())
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    @Override
    public void publish(EventType eventType, String eventData) {
        // getOrDefault — if nobody subscribed to this event, return empty list safely.
        // Avoids NullPointerException if no listeners are registered.
        List<EventListener> eventListeners = listeners.getOrDefault(eventType, List.of());
        for (EventListener listener : eventListeners) {
            listener.onEvent(eventData); // notify each listener
        }
    }

    @Override
    public void unsubscribeAll(EventType eventType) {
        listeners.remove(eventType);
    }
}
