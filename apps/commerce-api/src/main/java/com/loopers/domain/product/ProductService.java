package com.loopers.domain.product;

import com.loopers.domain.order.OrderCommand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        );
    }

    @Transactional
    public void decreaseStock(List<Product> products, List<OrderCommand.CreateItem> items) {
        for (OrderCommand.CreateItem item : items) {
            Product product = findProductById(products, item.productId());
            product.decreaseStock(item.quantity());
        }
    }

    private Product findProductById(List<Product> products, Long productId) {
        return products.stream()
                .filter(product -> product.getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }
}
