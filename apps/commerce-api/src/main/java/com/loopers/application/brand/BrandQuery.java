package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrandQuery {

    private final BrandQueryRepository brandQueryRepository;

    @Autowired
    public BrandQuery(BrandQueryRepository brandQueryRepository) {
        this.brandQueryRepository = brandQueryRepository;
    }

    public BrandQueryResult getBrandInfo(Long brandId) {
        return brandQueryRepository.findBrandInfoById(brandId)
                .map(data -> new BrandQueryResult(data.id(), data.name(), data.description()))
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
    }

    public record BrandQueryResult(
            Long id,
            String name,
            String description
    ) {
        public static BrandQueryResult from(Brand brand) {
            return new BrandQueryResult(
                    brand.getId(),
                    brand.getName(),
                    brand.getDescription());
        }
    }
}
