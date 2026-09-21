package com.ecommerce.product_service.aspect;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)       // can only be placed on methods
@Retention(RetentionPolicy.RUNTIME) // available at runtime for reflection
public @interface TrackExecutionTime {
}
