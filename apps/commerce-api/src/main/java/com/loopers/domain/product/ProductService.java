package com.loopers.domain.product;

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

    @Transactional(readOnly = true)
    public List<Product> findProductsByIds(List<Long> productIds) {
        List<Product> products = productIds.stream()
                .map(this::findById)
                .toList();

        if (products.size() != productIds.size()) {
            throw new CoreException(ErrorType.NOT_FOUND, "일부 상품을 찾을 수 없습니다.");
        }

        return products;
    }

}
