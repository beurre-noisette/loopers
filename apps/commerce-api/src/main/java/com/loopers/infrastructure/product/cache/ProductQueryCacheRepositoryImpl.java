package com.loopers.infrastructure.product.cache;

import com.loopers.application.product.ProductQuery;
import com.loopers.application.product.ProductQueryCacheRepository;
import com.loopers.support.cache.CacheConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Slf4j
public class ProductQueryCacheRepositoryImpl implements ProductQueryCacheRepository {
    
    private final RedisTemplate<String, Object> objectRedisTemplate;
    
    public ProductQueryCacheRepositoryImpl(RedisTemplate<String, Object> objectRedisTemplate) {
        this.objectRedisTemplate = objectRedisTemplate;
    }
    
    @Override
    public Optional<ProductQuery.ProductDetailResult> findDetailById(Long productId) {
        String cacheKey = buildDetailCacheKey(productId);
        
        try {
            ProductQuery.ProductDetailResult cachedResult = 
                (ProductQuery.ProductDetailResult) objectRedisTemplate.opsForValue().get(cacheKey);
            
            if (cachedResult != null) {
                log.debug("Query Cache Hit - Product ID: {}", productId);
                return Optional.of(cachedResult);
            } else {
                log.debug("Query Cache Miss - Product ID: {}", productId);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.warn("Query 캐시 조회 실패 - Product ID: {}, 원인: {}", productId, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public void saveDetail(Long productId, ProductQuery.ProductDetailResult result) {
        String cacheKey = buildDetailCacheKey(productId);
        
        try {
            objectRedisTemplate.opsForValue().set(
                cacheKey, 
                result, 
                CacheConstants.TTL.PRODUCT_DETAIL
            );
            log.debug("Query 캐시 저장 완료 - Product ID: {}, TTL: {}분", 
                productId, CacheConstants.TTL.PRODUCT_DETAIL.toMinutes());
                
        } catch (Exception e) {
            log.error("Query 캐시 저장 실패 - Product ID: {}, 원인: {}", productId, e.getMessage());
        }
    }
    
    @Override
    public void evictDetail(Long productId) {
        String cacheKey = buildDetailCacheKey(productId);
        
        try {
            Boolean deleted = objectRedisTemplate.delete(cacheKey);
            log.debug("Query 캐시 무효화 - Product ID: {}, 삭제 성공: {}", productId, deleted);
            
        } catch (Exception e) {
            log.error("Query 캐시 무효화 실패 - Product ID: {}, 원인: {}", productId, e.getMessage());
        }
    }
    
    private String buildDetailCacheKey(Long productId) {
        return CacheConstants.Keys.PRODUCT_DETAIL_DTO + productId;
    }
}
