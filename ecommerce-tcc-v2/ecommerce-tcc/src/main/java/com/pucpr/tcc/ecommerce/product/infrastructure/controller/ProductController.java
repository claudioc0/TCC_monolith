package com.pucpr.tcc.ecommerce.product.infrastructure.controller;

import com.pucpr.tcc.ecommerce.product.application.ProductService;
import com.pucpr.tcc.ecommerce.product.infrastructure.dto.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody ProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductResponse.from(productService.create(req.name(), req.price(), req.stockQuantity())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ProductResponse.from(productService.findById(id)));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll() {
        return ResponseEntity.ok(productService.findAll().stream().map(ProductResponse::from).toList());
    }

    @PatchMapping("/{id}/stock/decrease")
    public ResponseEntity<Void> decreaseStock(@PathVariable Long id, @RequestBody StockDecreaseRequest req) {
        productService.decreaseStock(id, req.quantity());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stock/check")
    public ResponseEntity<Boolean> checkStock(@PathVariable Long id, @RequestParam int quantity) {
        return ResponseEntity.ok(productService.checkStock(id, quantity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
