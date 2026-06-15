package com.pucpr.tcc.monolith.users.dto;

import com.pucpr.tcc.monolith.users.entity.User;
import java.time.LocalDateTime;

/** DTO de saída com informações públicas do usuário. */
public record UserResponse(Long id, String name, String email, String role, LocalDateTime createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(),
                u.getRole().name(), u.getCreatedAt());
    }
}
