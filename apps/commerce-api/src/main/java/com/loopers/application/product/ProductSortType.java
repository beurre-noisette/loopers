package com.loopers.application.product;

import lombok.Getter;

@Getter
public enum ProductSortType {
    LATEST("latest"),           // 최신순
    PRICE_ASC("price_asc"),     // 가격 오름차순
    LIKES_DESC("likes_desc");   // 좋아요 많은순
    
    private final String value;
    
    ProductSortType(String value) {
        this.value = value;
    }

    public static ProductSortType from(String value) {
        if (value == null) {
            return LATEST;
        }
        
        for (ProductSortType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        return LATEST; // 기본값
    }
}
