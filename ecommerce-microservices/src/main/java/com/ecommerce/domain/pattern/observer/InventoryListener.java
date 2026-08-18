package com.ecommerce.domain.pattern.observer;

public class InventoryListener implements EventListener {

    @Override
    public void onEvent(String eventData) {
        System.out.println("[INVENTORY] Updating stock for event: " + eventData);
    }
}
