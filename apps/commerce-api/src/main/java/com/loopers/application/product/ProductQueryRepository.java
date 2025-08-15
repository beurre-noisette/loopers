package com.loopers.application.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductQueryRepository {
    
    Page<ProductQueryData> findProducts(Long brandId, ProductSortType sortType, Pageable pageable);
    
    Optional<ProductDetailQueryData> findProductDetailById(Long productId);
    
    record ProductQueryData(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Long brandId,
        String brandName,
        Integer likeCount
    ) {}
    
    record ProductDetailQueryData(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Long brandId,
        String brandName,
        String brandDescription,
        Integer likeCount
    ) {}
}
