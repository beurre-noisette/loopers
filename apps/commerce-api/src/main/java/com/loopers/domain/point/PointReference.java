package com.loopers.domain.point;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

@Embeddable
@Getter
public class PointReference {

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type")
    private ReferenceType type;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_detail")
    private String detail;

    protected PointReference() {}

    private PointReference(ReferenceType type, Long referenceId, String detail) {
        this.type = type;
        this.referenceId = referenceId;
        this.detail = detail;
    }

    public static PointReference order(Long orderId) {
        return new PointReference(ReferenceType.ORDER, orderId, null);
    }

    public static PointReference userCharge() {
        return new PointReference(ReferenceType.USER_CHARGE, null, "사용자 직접 충전");
    }

    public static PointReference userRegistration() {
        return new PointReference(ReferenceType.SYSTEM_BONUS, null, "회원가입");
    }

    public static PointReference welcomeBonus() {
        return new PointReference(ReferenceType.WELCOME_BONUS, null, "신규 가입 보너스");
    }

    public static PointReference adminAdjustment(String reason) {
        return new PointReference(ReferenceType.ADMIN, null, reason);
    }
}
