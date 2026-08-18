package com.ecommerce.domain.pattern.observer;

public class SmsNotificationListener implements EventListener {

    @Override
    public void onEvent(String eventData) {
        System.out.println("[SMS] Sending SMS for event: " + eventData);
    }
}
