package com.pucpr.tcc.ecommerce.order.infrastructure.repository;

import com.pucpr.tcc.ecommerce.order.domain.Order;
import com.pucpr.tcc.ecommerce.order.domain.OrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaOrderRepository extends JpaRepository<Order, Long>, OrderRepository {
    List<Order> findByCustomerId(Long customerId);
}
