package com.loopers.application.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ProductQuery {
    
    private final ProductQueryRepository productQueryRepository;
    
    @Autowired
    public ProductQuery(ProductQueryRepository productQueryRepository) {
        this.productQueryRepository = productQueryRepository;
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
    
    public record ProductQueryResult(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Long brandId,
        String brandName,
        Long likeCount
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
        Long likeCount
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