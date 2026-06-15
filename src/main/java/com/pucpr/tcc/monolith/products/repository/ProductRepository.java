package com.pucpr.tcc.monolith.products.repository;

import com.pucpr.tcc.monolith.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório JPA para a entidade Product.
 * Spring Data gera a implementação automaticamente em tempo de execução.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Busca produtos cujo nome contenha o termo informado (case-insensitive). */
    List<Product> findByNameContainingIgnoreCase(String name);

    /** Busca produtos com estoque maior que zero (disponíveis para venda). */
    List<Product> findByStockQuantityGreaterThan(int minStock);
}
