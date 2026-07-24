package com.pucpr.tcc.ecommerce.product.application;

import com.pucpr.tcc.ecommerce.product.domain.Product;
import com.pucpr.tcc.ecommerce.product.domain.ProductNotFoundException;
import com.pucpr.tcc.ecommerce.product.domain.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product create(String name, BigDecimal price, Integer stockQuantity) {
        return productRepository.save(new Product(name, price, stockQuantity));
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product updatePrice(Long id, BigDecimal newPrice) {
        Product product = findById(id);
        product.updatePrice(newPrice);
        return productRepository.save(product);
    }

    public void decreaseStock(Long id, int quantity) {
        Product product = findById(id);
        product.decreaseStock(quantity);
        productRepository.save(product);
    }

    public boolean checkStock(Long id, int quantity) {
        return findById(id).hasStock(quantity);
    }

    public void delete(Long id) {
        findById(id);
        productRepository.deleteById(id);
    }
}
