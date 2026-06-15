package com.pucpr.tcc.monolith.security;

import com.pucpr.tcc.monolith.users.entity.User;
import com.pucpr.tcc.monolith.users.entity.UserRole;
import com.pucpr.tcc.monolith.users.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DatabaseSeeder {

    @Bean
    public CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("admin@tcc.com").isEmpty()) {
                User admin = new User(
                        "Administrador",
                        "admin@tcc.com",
                        passwordEncoder.encode("admin123"),
                        UserRole.ADMIN
                );
                userRepository.save(admin);
            }

            if (userRepository.findByEmail("joao@email.com").isEmpty()) {
                User customer = new User(
                        "João Silva",
                        "joao@email.com",
                        passwordEncoder.encode("cliente123"),
                        UserRole.CUSTOMER
                );
                userRepository.save(customer);
            }
        };
    }
}