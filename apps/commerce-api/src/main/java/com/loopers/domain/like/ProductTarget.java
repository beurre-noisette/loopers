package com.loopers.domain.like;

public class ProductTarget extends Target {
    
    private ProductTarget(Long productId) {
        super(productId);
    }
    
    public static ProductTarget of(Long productId) {
        return new ProductTarget(productId);
    }
    
    @Override
    public TargetType getType() {
        return TargetType.PRODUCT;
    }
    
    public Long getProductId() {
        return getId();
    }
}