package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "brand")
@Getter
public class Brand extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    protected Brand() {}

    private Brand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static Brand of(BrandCommand.Create command) {
        validateBrandName(command.name());

        return new Brand(command.name(), command.description());
    }

    public static void validateBrandName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 비어있을 수 없습니다.");
        }
    }
}
