package com.loopers.application.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
public class ProductQuery {
    
    private final ProductQueryRepository productQueryRepository;
    private final ProductQueryCacheRepository productQueryCacheRepository;
    
    @Autowired
    public ProductQuery(ProductQueryRepository productQueryRepository,
                       ProductQueryCacheRepository productQueryCacheRepository) {
        this.productQueryRepository = productQueryRepository;
        this.productQueryCacheRepository = productQueryCacheRepository;
    }
    
    public ProductListResult getProducts(Long brandId, String sort, int page, int size) {
        ProductSortType sortType = ProductSortType.from(sort);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<ProductQueryRepository.ProductQueryData> productPage = 
            productQueryRepository.findProducts(brandId, sortType, pageable);
        
        List<ProductQueryResult> products = productPage.getContent().stream()
            .map(ProductQueryResult::from)
            .toList();
        
        return new ProductListResult(
            products,
            productPage.getTotalElements(),
            productPage.getTotalPages(),
            productPage.getNumber(),
            productPage.getSize()
        );
    }
    
    public ProductDetailResult getProductDetail(Long productId) {
        return productQueryRepository.findProductDetailById(productId)
            .map(ProductDetailResult::from)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }
    
    public ProductDetailResult getProductDetailWithCache(Long productId) {
        log.debug("DTO 캐시를 사용한 상품 상세 조회 시작 - Product ID: {}", productId);
        
        return productQueryCacheRepository.findDetailById(productId)
                .orElseGet(() -> {
                    log.debug("Cache Miss - Product ID: {}, DB에서 조회", productId);
                    
                    ProductDetailResult result = productQueryRepository.findProductDetailById(productId)
                            .map(ProductDetailResult::from)
                            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
                    
                    productQueryCacheRepository.saveDetail(productId, result);
                    
                    return result;
                });
    }
    
    public void evictProductDetailCache(Long productId) {
        productQueryCacheRepository.evictDetail(productId);
    }
    
    public record ProductQueryResult(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Long brandId,
        String brandName,
        Integer likeCount
    ) {
        public static ProductQueryResult from(ProductQueryRepository.ProductQueryData data) {
            return new ProductQueryResult(
                data.id(),
                data.name(),
                data.description(),
                data.price(),
                data.stock(),
                data.brandId(),
                data.brandName(),
                data.likeCount()
            );
        }
    }
    
    public record ProductDetailResult(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        BrandInfo brand,
        Integer likeCount
    ) {
        public static ProductDetailResult from(ProductQueryRepository.ProductDetailQueryData data) {
            return new ProductDetailResult(
                data.id(),
                data.name(),
                data.description(),
                data.price(),
                data.stock(),
                new BrandInfo(data.brandId(), data.brandName(), data.brandDescription()),
                data.likeCount()
            );
        }
    }
    
    public record BrandInfo(
        Long id,
        String name,
        String description
    ) {}
    
    public record ProductListResult(
        List<ProductQueryResult> products,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
    ) {}
}
