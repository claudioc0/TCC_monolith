package com.pucpr.tcc.monolith.users.controller;

import com.pucpr.tcc.monolith.users.dto.AuthResponse;
import com.pucpr.tcc.monolith.users.dto.LoginRequest;
import com.pucpr.tcc.monolith.users.dto.RegisterRequest;
import com.pucpr.tcc.monolith.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de autenticação.
 *
 * Endpoints públicos (não requerem JWT):
 * - POST /api/auth/register → cadastra novo usuário e retorna token
 * - POST /api/auth/login    → autentica e retorna token
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Cadastra um novo usuário com role CUSTOMER.
     * Retorna 201 Created com o token JWT para que o cliente
     * já possa fazer chamadas autenticadas imediatamente.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    /**
     * Autentica um usuário existente.
     * Retorna 200 OK com o token JWT em caso de sucesso,
     * ou 401 Unauthorized se as credenciais forem inválidas.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }
}
