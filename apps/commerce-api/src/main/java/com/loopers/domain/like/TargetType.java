package com.loopers.domain.like;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

@Getter
public enum TargetType {
    PRODUCT("PRODUCT");

    private final String value;
    
    TargetType(String value) {
        this.value = value;
    }

    public static TargetType from(String value) {
        if (value == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "TargetType value는 null일 수 없습니다.");
        }
        
        for (TargetType targetType : values()) {
            if (targetType.value.equals(value)) {
                return targetType;
            }
        }
        
        throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 TargetType입니다: " + value);
    }
}
