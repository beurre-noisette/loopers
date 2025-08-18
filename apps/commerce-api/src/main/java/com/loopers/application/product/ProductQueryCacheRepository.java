package com.loopers.application.product;

import java.util.Optional;

public interface ProductQueryCacheRepository {
    
    Optional<ProductQuery.ProductDetailResult> findDetailById(Long productId);
    
    void saveDetail(Long productId, ProductQuery.ProductDetailResult result);
    
    void evictDetail(Long productId);
}
