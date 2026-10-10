package com.smartpay.platform.mapper;

import com.smartpay.platform.dto.UserResponse;
import com.smartpay.platform.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(), u.getStatus(),
                u.getCreatedAt());
    }
}
