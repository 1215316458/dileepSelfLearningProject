package com.ecommerce.user_service.dto;

import com.ecommerce.user_service.domain.entity.User;
import com.ecommerce.user_service.domain.enums.Role;

import java.time.Instant;
import java.util.Set;

// Read-only view of a user — never expose the password hash
public record UserResponse(
    Long    id,
    String  username,
    String  email,
    Set<Role> roles,
    boolean enabled,
    Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRoles(),
            user.isEnabled(),
            user.getCreatedAt()
        );
    }
}
