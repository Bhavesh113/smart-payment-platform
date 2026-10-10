package com.smartpay.platform.dto;

import com.smartpay.platform.entity.Role;
import com.smartpay.platform.entity.UserStatus;
import java.time.Instant;

public record UserResponse(Long id, String name, String email, String phone, Role role, UserStatus status,
                           Instant createdAt) {
}
