package com.pucpr.tcc.ecommerce.user.infrastructure.dto;

import com.pucpr.tcc.ecommerce.user.domain.Role;
import com.pucpr.tcc.ecommerce.user.domain.User;

public record UserResponse(Long id, String username, Role role) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getRole());
    }
}
