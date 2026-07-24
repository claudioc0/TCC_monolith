package com.pucpr.tcc.ecommerce.user.infrastructure.repository;

import com.pucpr.tcc.ecommerce.user.domain.User;
import com.pucpr.tcc.ecommerce.user.domain.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaUserRepository extends JpaRepository<User, Long>, UserRepository {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
