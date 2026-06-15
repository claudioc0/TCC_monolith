package com.pucpr.tcc.monolith.orders.service;

import com.pucpr.tcc.monolith.orders.dto.OrderRequest;
import com.pucpr.tcc.monolith.orders.dto.OrderResponse;
import com.pucpr.tcc.monolith.orders.dto.UpdateStatusRequest;
import com.pucpr.tcc.monolith.orders.entity.Order;
import com.pucpr.tcc.monolith.orders.entity.OrderItem;
import com.pucpr.tcc.monolith.orders.entity.OrderStatus;
import com.pucpr.tcc.monolith.orders.exception.OrderNotFoundException;
import com.pucpr.tcc.monolith.orders.repository.OrderRepository;
import com.pucpr.tcc.monolith.products.entity.Product;
import com.pucpr.tcc.monolith.products.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Camada de negócio para Pedidos.
 *
 * Responsabilidades:
 * - Orquestrar a criação de pedidos (validação de estoque + desconto do mesmo).
 * - Listar pedidos por usuário ou por status.
 * - Gerenciar cancelamento (com devolução de estoque).
 * - Delegar transições de status para a entidade Order.
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    public OrderService(OrderRepository orderRepository, ProductService productService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    // ── Criação ───────────────────────────────────────────────

    /**
     * Cria um pedido:
     * 1. Valida e desconta o estoque de cada produto.
     * 2. Cria os OrderItems com snapshot de preço.
     * 3. Persiste o pedido com status PENDENTE.
     *
     * @param userId ID do usuário autenticado (extraído do JWT no Controller).
     * @param request DTO com os itens solicitados.
     * @return DTO com o pedido criado.
     */
    public OrderResponse create(Long userId, OrderRequest request) {
        Order order = new Order(userId);

        for (OrderRequest.OrderItemRequest itemReq : request.items()) {
            // Busca o produto (lança ProductNotFoundException se não existir)
            Product product = productService.getProductOrThrow(itemReq.productId());

            // Desconta o estoque (lança InsufficientStockException se insuficiente)
            productService.decreaseStock(itemReq.productId(), itemReq.quantity());

            // Cria o item com snapshot do preço atual
            OrderItem item = new OrderItem(
                order,
                product.getId(),
                product.getName(),
                product.getPrice(),
                itemReq.quantity()
            );
            order.addItem(item);
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    // ── Consultas ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return OrderResponse.from(getOrderOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByUser(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll()
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    // ── Atualização de Status ─────────────────────────────────

    /**
     * Atualiza o status de um pedido seguindo o fluxo:
     * PENDENTE → CONFIRMADO → ENVIADO → ENTREGUE
     *
     * A validação da transição é delegada ao método {@link Order#transitionTo(OrderStatus)},
     * que encapsula a regra de negócio no domínio.
     *
     * @throws com.pucpr.tcc.monolith.orders.exception.InvalidStatusTransitionException se a transição for ilegal.
     */
    public OrderResponse updateStatus(Long orderId, UpdateStatusRequest request) {
        Order order = getOrderOrThrow(orderId);
        order.transitionTo(request.newStatus());
        return OrderResponse.from(orderRepository.save(order));
    }

    // ── Cancelamento ──────────────────────────────────────────

    /**
     * Cancela um pedido e devolve o estoque de cada item ao produto.
     * Só é permitido cancelar pedidos PENDENTE ou CONFIRMADO (regra no enum).
     */
    public OrderResponse cancel(Long orderId) {
        Order order = getOrderOrThrow(orderId);

        // Utiliza o mesmo mecanismo de transição — garante consistência
        order.transitionTo(OrderStatus.CANCELADO);

        // Devolve o estoque de cada item
        for (OrderItem item : order.getItems()) {
            productService.increaseStock(item.getProductId(), item.getQuantity());
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    // ── Helper ────────────────────────────────────────────────

    private Order getOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
