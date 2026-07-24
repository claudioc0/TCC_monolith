package com.pucpr.tcc.ecommerce.user.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    protected User() {}

    public User(String username, String password, Role role) {
        if (username == null || username.isBlank()) {
            throw new InvalidUserException("Username não pode ser vazio.");
        }
        if (password == null || password.isBlank()) {
            throw new InvalidUserException("Password não pode ser vazio.");
        }
        this.username = username;
        this.password = password;
        this.role = role == null ? Role.USER : role;
    }

    public boolean hasRole(Role candidate) {
        return this.role == candidate;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
}
