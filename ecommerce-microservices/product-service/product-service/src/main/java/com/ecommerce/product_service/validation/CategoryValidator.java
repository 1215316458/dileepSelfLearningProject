package com.ecommerce.product_service.validation;

import com.ecommerce.product_service.domain.enums.Category;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CategoryValidator implements ConstraintValidator<ValidCategory, Category> {

    @Override
    public boolean isValid(Category category, ConstraintValidatorContext context) {
        // null is handled by @NotNull — this validator only checks non-null values
        if (category == null) return true;
        // since Category is an enum, any non-null value is already a valid constant
        // this validator exists to demonstrate the pattern and provide a custom message
        for (Category valid : Category.values()) {
            if (valid == category) return true;
        }
        return false;
    }
}
