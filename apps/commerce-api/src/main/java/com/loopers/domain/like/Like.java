package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "likes", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "target_type", "target_id"}))
@Getter
public class Like extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;
    
    @Column(name = "target_id", nullable = false)
    private Long targetId;
    
    @Version
    private Long version;
    
    protected Like() {}
    
    private Like(User user, TargetType targetType, Long targetId) {
        this.user = user;
        this.targetType = targetType;
        this.targetId = targetId;
    }
    
    public static Like of(User user, Target target) {
        if (user == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자는 필수입니다.");
        }

        if (target == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Target은 필수입니다.");
        }
        
        return new Like(user, target.getType(), target.getId());
    }
    
    public Target getTarget() {
        return switch (targetType) {
            case PRODUCT -> ProductTarget.of(targetId);
        };
    }
    
    public Long getUserId() {
        return user.getId();
    }
    
    public boolean isTargetType(TargetType targetType) {
        return this.targetType == targetType;
    }
    
    public boolean isTarget(Target target) {
        return this.targetType == target.getType() && this.targetId.equals(target.getId());
    }
}
