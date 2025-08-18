package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductQuery;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Product V1 API", description = "상품 조회")
public interface ProductV1ApiSpec {

    @Operation(
            summary = "상품 목록 조회",
            description = "브랜드별, 정렬별로 상품 목록을 페이징하여 조회합니다."
    )
    ApiResponse<ProductQuery.ProductListResult> getProducts(
            @Parameter(description = "브랜드 ID (선택사항)")
            Long brandId,
            
            @Parameter(description = "정렬 기준 (latest/price_asc/price_desc/likes_desc)", 
                      schema = @Schema(defaultValue = "latest"))
            String sort,
            
            @Parameter(description = "페이지 번호", 
                      schema = @Schema(defaultValue = "0"))
            int page,
            
            @Parameter(description = "페이지 크기", 
                      schema = @Schema(defaultValue = "20"))
            int size
    );

    @Operation(
            summary = "상품 상세 조회",
            description = "상품 ID로 상품의 상세 정보를 조회합니다."
    )
    ApiResponse<ProductQuery.ProductDetailResult> getProductDetail(
            @Parameter(description = "상품 ID", required = true)
            Long productId
    );
}