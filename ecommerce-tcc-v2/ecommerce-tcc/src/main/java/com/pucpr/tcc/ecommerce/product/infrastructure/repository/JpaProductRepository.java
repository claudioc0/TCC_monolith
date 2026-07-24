package com.pucpr.tcc.ecommerce.product.infrastructure.repository;

import com.pucpr.tcc.ecommerce.product.domain.Product;
import com.pucpr.tcc.ecommerce.product.domain.ProductRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaProductRepository extends JpaRepository<Product, Long>, ProductRepository {}
