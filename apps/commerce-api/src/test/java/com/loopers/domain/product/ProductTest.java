package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ProductTest {

    @DisplayName("상품(Product) 필드 접근 시")
    @Nested
    class FieldAccess {
        
        @DisplayName("모든 필드가 올바르게 조회된다.")
        @Test
        void getAllFields_whenProductCreated() {
            // arrange
            Product product = new Product();
            Brand brand = Mockito.mock(Brand.class);
            
            String expectedName = "iPhone 15 Pro";
            String expectedDescription = "티타늄 소재로 제작된 프리미엄 스마트폰";
            BigDecimal expectedPrice = new BigDecimal("1490000.00");
            Integer expectedStock = 50;
            
            // act
            ReflectionTestUtils.setField(product, "name", expectedName);
            ReflectionTestUtils.setField(product, "description", expectedDescription);
            ReflectionTestUtils.setField(product, "price", expectedPrice);
            ReflectionTestUtils.setField(product, "stock", expectedStock);
            ReflectionTestUtils.setField(product, "brand", brand);
            
            // assert
            assertAll(
                () -> assertThat(product.getName()).isEqualTo(expectedName),
                () -> assertThat(product.getDescription()).isEqualTo(expectedDescription),
                () -> assertThat(product.getPrice()).isEqualTo(expectedPrice),
                () -> assertThat(product.getStock()).isEqualTo(expectedStock),
                () -> assertThat(product.getBrand()).isEqualTo(brand)
            );
        }
        
        @DisplayName("상품명이 올바르게 조회된다.")
        @Test
        void getName_whenNameIsSet() {
            // arrange
            Product product = new Product();
            String expectedName = "MacBook Pro 14";
            ReflectionTestUtils.setField(product, "name", expectedName);
            
            // act
            String actualName = product.getName();
            
            // assert
            assertThat(actualName).isEqualTo(expectedName);
        }
        
        @DisplayName("상품 설명이 올바르게 조회된다.")
        @Test
        void getDescription_whenDescriptionIsSet() {
            // assert
            Product product = new Product();
            String expectedDescription = "M3 Pro 칩셋이 탑재된 고성능 노트북";
            ReflectionTestUtils.setField(product, "description", expectedDescription);
            
            // act
            String actualDescription = product.getDescription();
            
            // assert
            assertThat(actualDescription).isEqualTo(expectedDescription);
        }
        
        @DisplayName("상품 가격이 올바르게 조회된다.")
        @Test
        void getPrice_whenPriceIsSet() {
            // arrange
            Product product = new Product();
            BigDecimal expectedPrice = new BigDecimal("2690000.00");
            ReflectionTestUtils.setField(product, "price", expectedPrice);
            
            // act
            BigDecimal actualPrice = product.getPrice();
            
            // assert
            assertThat(actualPrice).isEqualTo(expectedPrice);
        }
        
        @DisplayName("상품 재고가 올바르게 조회된다.")
        @Test
        void getStock_whenStockIsSet() {
            // arrange
            Product product = new Product();
            Integer expectedStock = 30;
            ReflectionTestUtils.setField(product, "stock", expectedStock);
            
            // act
            Integer actualStock = product.getStock();
            
            // assert
            assertThat(actualStock).isEqualTo(expectedStock);
        }
    }
    
    @DisplayName("브랜드 연관관계 시")
    @Nested
    class BrandRelation {
        
        @DisplayName("브랜드가 올바르게 조회된다.")
        @Test
        void getBrand_whenBrandIsSet() {
            // arrange
            Product product = new Product();
            Brand brand = Mockito.mock(Brand.class);
            when(brand.getName()).thenReturn("Apple");
            ReflectionTestUtils.setField(product, "brand", brand);
            
            // act
            Brand actualBrand = product.getBrand();
            
            // assert
            assertThat(actualBrand).isEqualTo(brand);
            assertThat(actualBrand.getName()).isEqualTo("Apple");
        }
        
        @DisplayName("브랜드가 null일 때 null을 반환한다.")
        @Test
        void getBrand_whenBrandIsNull() {
            // arrange
            Product product = new Product();
            ReflectionTestUtils.setField(product, "brand", null);
            
            // act
            Brand actualBrand = product.getBrand();
            
            // assert
            assertThat(actualBrand).isNull();
        }
    }
}
