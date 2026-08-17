package com.ecommerce.domain.model;

import java.time.LocalDateTime;

public abstract class BaseEntity<ID> {

    // private — no one outside this class should directly touch these fields
    private ID identity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ID getIdentity() { return identity; }
    public void setIdentity(ID identity) { this.identity = identity; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        // StringBuilder avoids creating a new String object for every + concatenation
        return new StringBuilder("BaseEntity{")
                .append("identity=").append(identity)
                .append(", createdAt=").append(createdAt)
                .append(", updatedAt=").append(updatedAt)
                .append('}')
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity<?>)) return false;
        BaseEntity<?> that = (BaseEntity<?>) o;
        return identity != null ? identity.equals(that.identity) : that.identity == null;
    }

    @Override
    public int hashCode() {
        // consistent with equals — two equal objects must have the same hashCode
        return identity != null ? identity.hashCode() : 0;
    }
}
