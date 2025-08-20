package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "product", indexes = {
    @Index(name = "idx_product_brand_created", columnList = "brand_id, created_at DESC"),
    @Index(name = "idx_product_brand_price", columnList = "brand_id, price"),
    @Index(name = "idx_product_like_count", columnList = "like_count DESC")
})
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

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    protected Product() {}

    private Product(String name, String description, BigDecimal price, Integer stock, Brand brand) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.brand = brand;
        this.likeCount = 0;
    }

    public static Product of(ProductCommand.Create command, Brand brand) {
        validateName(command.name());
        validateDescription(command.description());
        validatePrice(command.price());
        validateStock(command.stock());
        validateBrand(brand);

        return new Product(command.name(), command.description(), command.price(), command.stock(), brand);
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
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

    public boolean hasEnoughStock(Integer requiredQuantity) {
        return this.stock >= requiredQuantity;
    }

    public void validateStockAvailability(Integer quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 수량은 0보다 커야 합니다.");
        }

        if (!hasEnoughStock(quantity)) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    String.format("재고 부족 - 상품: %s, 요청: %s, 재고: %s",
                            this.name, quantity, this.stock));
        }
    }

    public static Product findById(List<Product> products, Long productId) {
        return products.stream()
                .filter(product -> product.getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다. 상품 ID: " + productId));
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, "차감할 수량은 1개 이상이어야 합니다.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 비어있을 수 없습니다.");
        }
    }
    
    private static void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 설명은 비어있을 수 없습니다.");
        }
    }
    
    private static void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 가격은 0원보다 커야 합니다.");
        }
    }
    
    private static void validateStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 0개 이상이어야 합니다.");
        }
    }
    
    private static void validateBrand(Brand brand) {
        if (brand == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 정보는 필수입니다.");
        }
    }

}
