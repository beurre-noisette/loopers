package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductQuery;
import com.loopers.interfaces.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductQuery productQuery;

    @Autowired
    public ProductV1Controller(ProductQuery productQuery) {
        this.productQuery = productQuery;
    }

    @GetMapping
    public ApiResponse<ProductQuery.ProductListResult> getProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(productQuery.getProducts(brandId, sort, page, size));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductQuery.ProductDetailResult> getProductDetail(@PathVariable Long productId) {
        return ApiResponse.success(productQuery.getProductDetailWithCache(productId));
    }

    @GetMapping("/{productId}/no-cache")
    public ApiResponse<ProductQuery.ProductDetailResult> getProductDetailNoCache(@PathVariable Long productId) {
        return ApiResponse.success(productQuery.getProductDetail(productId));
    }
}
