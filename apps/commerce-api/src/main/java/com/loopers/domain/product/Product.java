package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
@Getter
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    protected Product() {}

    private Product(String name, String description, BigDecimal price, Integer stock, Brand brand) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.brand = brand;
    }

    public void decreaseStock(int quantity) {
        validateQuantity(quantity);

        if (this.stock < quantity) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT,
                    "재고가 부족합니다. 상품: " + this.name + 
                    ", 현재 재고: " + this.stock + ", 요청 수량: " + quantity);
        }

        this.stock -= quantity;
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "차감할 수량은 1개 이상이어야 합니다.");
        }
    }

}
