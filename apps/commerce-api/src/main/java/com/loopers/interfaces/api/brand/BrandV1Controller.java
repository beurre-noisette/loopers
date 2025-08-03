package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandQuery;
import com.loopers.interfaces.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/brands")
public class BrandV1Controller implements BrandV1ApiSpec {

    private final BrandQuery brandQuery;

    @Autowired
    public BrandV1Controller(BrandQuery brandQuery) {
        this.brandQuery = brandQuery;
    }

    @GetMapping("/{brandId}")
    public ApiResponse<BrandQuery.BrandQueryResult> getBrandInfo(@PathVariable Long brandId) {
        return ApiResponse.success(brandQuery.getBrandInfo(brandId));
    }
}
