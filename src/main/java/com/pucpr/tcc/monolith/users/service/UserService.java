package com.pucpr.tcc.monolith.users.service;

import com.pucpr.tcc.monolith.security.JwtService;
import com.pucpr.tcc.monolith.users.dto.AuthResponse;
import com.pucpr.tcc.monolith.users.dto.LoginRequest;
import com.pucpr.tcc.monolith.users.dto.RegisterRequest;
import com.pucpr.tcc.monolith.users.dto.UserResponse;
import com.pucpr.tcc.monolith.users.entity.User;
import com.pucpr.tcc.monolith.users.entity.UserRole;
import com.pucpr.tcc.monolith.users.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Camada de negócio para Usuários.
 *
 * Responsabilidades:
 * - Cadastrar novos usuários (com hash de senha BCrypt).
 * - Autenticar e emitir tokens JWT.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Cadastra um novo usuário com role CUSTOMER.
     *
     * @throws IllegalArgumentException se o e-mail já estiver em uso.
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado: " + request.email());
        }

        User user = new User(
            request.name(),
            request.email(),
            passwordEncoder.encode(request.password()), // senha nunca em texto plano
            UserRole.CUSTOMER
        );

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);

        return AuthResponse.of(token, saved.getId(), saved.getName(),
                               saved.getEmail(), saved.getRole().name());
    }

    /**
     * Autentica o usuário e retorna um token JWT.
     *
     * O {@link AuthenticationManager} valida as credenciais via
     * {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider},
     * que por sua vez usa o {@link UserDetailsService} configurado no SecurityConfig.
     *
     * @throws org.springframework.security.core.AuthenticationException se as credenciais forem inválidas.
     */
    public AuthResponse login(LoginRequest request) {
        // Lança BadCredentialsException automaticamente se inválido
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado após autenticação."));

        String token = jwtService.generateToken(user);

        return AuthResponse.of(token, user.getId(), user.getName(),
                               user.getEmail(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuário não encontrado com id: " + id);
        }
        userRepository.deleteById(id);
    }
}
