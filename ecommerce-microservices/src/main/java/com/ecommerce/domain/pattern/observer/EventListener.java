package com.ecommerce.domain.pattern.observer;

// @FunctionalInterface — exactly one abstract method.
// This allows listeners to be passed as lambdas:
//   publisher.subscribe(ORDER_PLACED, data -> System.out.println(data))
// Without this, you'd need a verbose anonymous class every time.
@FunctionalInterface
public interface EventListener {

    void onEvent(String eventData);
}
