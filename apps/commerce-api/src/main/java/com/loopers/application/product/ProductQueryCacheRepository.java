package com.loopers.application.product;

import java.util.Optional;

/**
 * Query Layer 전용 캐시 Repository 인터페이스
 * 
 * Application Layer에서 조회 성능 최적화를 위한 캐시 기능을 추상화
 * Domain Layer의 ProductCacheRepository와 달리 DTO 레벨에서 캐시를 관리
 * 
 * 특징:
 * - ProductDetailResult(DTO)를 직접 캐시
 * - QueryDSL로 조회된 결과물을 그대로 저장
 * - Application Layer가 Infrastructure에 직접 의존하지 않도록 추상화
 */
public interface ProductQueryCacheRepository {
    
    /**
     * 캐시에서 상품 상세 정보 조회 (DTO 형태)
     * 
     * @param productId 상품 ID
     * @return 캐시된 상품 상세 정보 (없으면 Optional.empty())
     */
    Optional<ProductQuery.ProductDetailResult> findDetailById(Long productId);
    
    /**
     * 캐시에 상품 상세 정보 저장 (DTO 형태)
     * TTL 설정은 구현체에서 결정
     * 
     * @param productId 상품 ID
     * @param result 저장할 상품 상세 정보
     */
    void saveDetail(Long productId, ProductQuery.ProductDetailResult result);
    
    /**
     * 캐시에서 상품 상세 정보 제거
     * 상품 정보가 변경되었을 때 호출 (좋아요 수 변경 등)
     * 
     * @param productId 제거할 상품 ID
     */
    void evictDetail(Long productId);
}