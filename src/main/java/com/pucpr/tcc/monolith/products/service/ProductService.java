package com.pucpr.tcc.monolith.products.service;

import com.pucpr.tcc.monolith.products.dto.ProductRequest;
import com.pucpr.tcc.monolith.products.dto.ProductResponse;
import com.pucpr.tcc.monolith.products.entity.Product;
import com.pucpr.tcc.monolith.products.exception.InsufficientStockException;
import com.pucpr.tcc.monolith.products.exception.ProductNotFoundException;
import com.pucpr.tcc.monolith.products.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Camada de negócio para Produtos.
 *
 * Responsabilidades:
 * - Criar, atualizar e remover produtos.
 * - Validar regras de estoque.
 * - Mapear entidades ↔ DTOs (nunca expondo entidades para a camada de Controller).
 */
@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ── Criação ───────────────────────────────────────────────

    /**
     * Cadastra um novo produto.
     * @param request DTO com os dados validados pelo Controller.
     * @return DTO de resposta com o produto persistido.
     */
    public ProductResponse create(ProductRequest request) {
        Product product = new Product(
            request.name(),
            request.description(),
            request.price(),
            request.stockQuantity()
        );
        return ProductResponse.from(productRepository.save(product));
    }

    // ── Consultas ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProductOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    // ── Atualização ───────────────────────────────────────────

    /**
     * Atualiza todos os campos de um produto existente.
     */
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProductOrThrow(id);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        return ProductResponse.from(productRepository.save(product));
    }

    // ── Remoção ───────────────────────────────────────────────

    public void delete(Long id) {
        getProductOrThrow(id); // Garante que o produto existe antes de remover
        productRepository.deleteById(id);
    }

    // ── Operações de Estoque (chamadas pelo OrderService) ─────

    /**
     * Reduz o estoque de um produto.
     * Chamado internamente pelo OrderService durante a criação de um pedido.
     *
     * @throws InsufficientStockException se a quantidade disponível for menor que a solicitada.
     */
    public void decreaseStock(Long productId, int quantity) {
        Product product = getProductOrThrow(productId);
        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(productId, quantity, product.getStockQuantity());
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
    }

    /**
     * Devolve estoque ao cancelar um pedido.
     * Chamado internamente pelo OrderService durante o cancelamento.
     */
    public void increaseStock(Long productId, int quantity) {
        Product product = getProductOrThrow(productId);
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Recupera a entidade Product ou lança {@link ProductNotFoundException}.
     * Reutilizado internamente para centralizar a lógica de "not found".
     */
    public Product getProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
