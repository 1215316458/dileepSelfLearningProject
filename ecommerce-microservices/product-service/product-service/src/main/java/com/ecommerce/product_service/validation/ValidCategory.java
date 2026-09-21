package com.ecommerce.product_service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CategoryValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCategory {
    String message() default "Invalid category. Must be one of: ELECTRONICS, BOOKS, CLOTHING, SPORTS, HOME";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
