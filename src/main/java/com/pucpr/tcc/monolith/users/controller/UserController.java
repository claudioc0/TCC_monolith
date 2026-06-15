package com.pucpr.tcc.monolith.users.controller;

import com.pucpr.tcc.monolith.users.dto.UserResponse;
import com.pucpr.tcc.monolith.users.entity.User;
import com.pucpr.tcc.monolith.users.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller de gerenciamento de usuários.
 *
 * Endpoints:
 * - GET /api/users/me          → perfil do usuário autenticado
 * - GET /api/users             → listar todos (somente ADMIN)
 * - GET /api/users/{id}        → buscar por id (somente ADMIN)
 * - DELETE /api/users/{id}     → remover (somente ADMIN)
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retorna o perfil do usuário atualmente autenticado.
     * Usa @AuthenticationPrincipal para evitar que o cliente
     * precise passar o próprio ID na URL.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(UserResponse.from(currentUser));
    }

    /**
     * Lista todos os usuários — acesso restrito a ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    /**
     * Busca um usuário por ID — acesso restrito a ADMIN.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * Remove um usuário por ID — acesso restrito a ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
