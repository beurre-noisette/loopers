package com.loopers.application.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("상품 정렬 타입 테스트")
class ProductSortTypeTest {

    @DisplayName("정렬 타입 문자열을 올바른 enum으로 변환한다")
    @Test
    void returnCorrectEnumValues() {
        assertThat(ProductSortType.from("latest")).isEqualTo(ProductSortType.LATEST);
        assertThat(ProductSortType.from("price_asc")).isEqualTo(ProductSortType.PRICE_ASC);
        assertThat(ProductSortType.from("likes_desc")).isEqualTo(ProductSortType.LIKES_DESC);
    }

    @DisplayName("대소문자를 구분하지 않고 변환한다")
    @Test
    void returnStringIgnoreCase() {
        assertThat(ProductSortType.from("LATEST")).isEqualTo(ProductSortType.LATEST);
        assertThat(ProductSortType.from("Price_Asc")).isEqualTo(ProductSortType.PRICE_ASC);
        assertThat(ProductSortType.from("LIKES_DESC")).isEqualTo(ProductSortType.LIKES_DESC);
    }

    @DisplayName("null이나 잘못된 값은 기본값(LATEST)을 반환한다")
    @Test
    void returnDefaultValue_whenProvidedNullOrInvalidValue() {
        assertThat(ProductSortType.from(null)).isEqualTo(ProductSortType.LATEST);
        assertThat(ProductSortType.from("")).isEqualTo(ProductSortType.LATEST);
        assertThat(ProductSortType.from("invalid")).isEqualTo(ProductSortType.LATEST);
        assertThat(ProductSortType.from("random")).isEqualTo(ProductSortType.LATEST);
    }
    
    @DisplayName("각 정렬 타입의 값을 올바르게 반환한다")
    @Test
    void getValue() {
        assertThat(ProductSortType.LATEST.getValue()).isEqualTo("latest");
        assertThat(ProductSortType.PRICE_ASC.getValue()).isEqualTo("price_asc");
        assertThat(ProductSortType.LIKES_DESC.getValue()).isEqualTo("likes_desc");
    }
}
