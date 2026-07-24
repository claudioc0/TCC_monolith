package com.pucpr.tcc.ecommerce.user.infrastructure.dto;

import com.pucpr.tcc.ecommerce.user.domain.Role;

public record RegisterRequest(String username, String password, Role role) {}
