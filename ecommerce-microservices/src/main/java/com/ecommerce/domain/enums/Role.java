package com.ecommerce.domain.enums;

public enum Role {

    CUSTOMER("Customer"),
    SELLER("Seller"),
    ADMIN("Admin");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
