package com.loopers.domain.like;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public abstract class Target {
    
    protected final Long id;
    
    protected Target(Long id) {
        if (id == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Target ID는 필수입니다.");
        }

        this.id = id;
    }
    
    public abstract TargetType getType();

}
