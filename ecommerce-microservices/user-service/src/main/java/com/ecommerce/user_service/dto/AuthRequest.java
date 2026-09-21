package com.ecommerce.user_service.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @NotBlank String username,   // accepts username OR email
    @NotBlank String password
) {}
