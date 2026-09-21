package com.ecommerce.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

// Only fields the user is allowed to update on their own profile
public record UpdateProfileRequest(
    @Email                 String email,
    @Size(min = 8)         String password   // null = don't change
) {}
