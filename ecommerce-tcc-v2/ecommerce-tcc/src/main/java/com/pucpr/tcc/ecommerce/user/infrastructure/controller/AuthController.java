package com.pucpr.tcc.ecommerce.user.infrastructure.controller;

import com.pucpr.tcc.ecommerce.security.JwtService;
import com.pucpr.tcc.ecommerce.user.application.UserService;
import com.pucpr.tcc.ecommerce.user.domain.User;
import com.pucpr.tcc.ecommerce.user.infrastructure.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest req) {
        User user = userService.register(req.username(), req.password(), req.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        User user = userService.authenticate(req.username(), req.password());
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
