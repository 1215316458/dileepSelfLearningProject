package com.ecommerce.domain.pattern.observer;

public class EmailNotificationListener implements EventListener {

    @Override
    public void onEvent(String eventData) {
        System.out.println("[EMAIL] Sending email for event: " + eventData);
    }
}
