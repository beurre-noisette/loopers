package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandQuery;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Brand V1 API", description = "브랜드 조회")
public interface BrandV1ApiSpec {

    @Operation(
            summary = "브랜드 조회",
            description = "브랜드의 이름과 브랜드의 설명에 대한 정보를 조회합니다."
    )
    ApiResponse<BrandQuery.BrandQueryResult> getBrandInfo(
            @Schema(name = "브랜드 ID", description = "브랜드의 식별자")
            Long brandId
    );
}
