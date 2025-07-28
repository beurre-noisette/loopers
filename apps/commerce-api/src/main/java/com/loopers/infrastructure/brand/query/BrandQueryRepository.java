package com.loopers.infrastructure.brand.query;

import java.util.Optional;

public interface BrandQueryRepository {
    
    Optional<BrandQueryData> findBrandInfoById(Long brandId);
    
    record BrandQueryData(
        Long id,
        String name,
        String description
    ) {}
}
