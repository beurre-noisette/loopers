package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 도메인 로직용 상품 조회 (캐시 없음)
     * 비즈니스 로직에서 사용하는 순수한 엔티티 조회
     */
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

    @Transactional
    public void increaseLikeCount(Long productId) {
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        product.increaseLikeCount();
        productRepository.save(product);
    }

    @Transactional
    public void decreaseLikeCount(Long productId) {
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        product.decreaseLikeCount();
        productRepository.save(product);
    }

}
