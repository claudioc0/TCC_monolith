package com.pucpr.tcc.monolith.orders.repository;

import com.pucpr.tcc.monolith.orders.entity.Order;
import com.pucpr.tcc.monolith.orders.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório JPA para a entidade Order.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Lista todos os pedidos de um usuário específico. */
    List<Order> findByUserId(Long userId);

    /** Lista pedidos por status. */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Lista pedidos criados dentro de um período.
     * Utilizado pelo ReportService para geração de relatórios.
     */
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Soma o totalAmount de pedidos não-cancelados em um período.
     * Retorna 0 se não houver pedidos no período.
     */
    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0)
        FROM Order o
        WHERE o.createdAt BETWEEN :start AND :end
          AND o.status <> 'CANCELADO'
        """)
    BigDecimal sumRevenueByPeriod(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    /**
     * Conta pedidos por status em um período.
     */
    @Query("""
        SELECT COUNT(o)
        FROM Order o
        WHERE o.createdAt BETWEEN :start AND :end
          AND o.status = :status
        """)
    long countByStatusAndPeriod(@Param("status") OrderStatus status,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);
}
