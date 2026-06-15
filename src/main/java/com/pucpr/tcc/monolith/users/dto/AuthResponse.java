package com.pucpr.tcc.monolith.users.dto;

/** DTO de saída após autenticação bem-sucedida. Contém o token JWT. */
public record AuthResponse(
    String token,
    String type,
    Long userId,
    String name,
    String email,
    String role
) {
    public static AuthResponse of(String token, Long userId, String name, String email, String role) {
        return new AuthResponse(token, "Bearer", userId, name, email, role);
    }
}
