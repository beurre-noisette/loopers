package com.loopers.domain.payment;

import com.loopers.domain.point.PointReference;
import com.loopers.domain.point.PointService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PointPaymentService implements PaymentService {

    private final PointService pointService;

    @Autowired
    public PointPaymentService(PointService pointService) {
        this.pointService = pointService;
    }

    @Override
    public void validatePaymentCapability(Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.PAYMENT_VALIDATION_FAILED, "결제 금액은 0보다 커야 합니다.");
        }
    }

    @Override
    public PaymentResult processPayment(Long userId, BigDecimal amount, PaymentReference reference) {
        try {
            validatePaymentCapability(userId, amount);

            PointReference pointReference = PointReference.order(reference.referenceId());

            pointService.usePoint(userId, amount, pointReference);

            return PaymentResult.success(generatePaymentId(), amount);

        } catch (CoreException e) {
            if (e.getErrorType() == ErrorType.NOT_ENOUGH) {
                throw new CoreException(ErrorType.PAYMENT_INSUFFICIENT_POINT);
            }
            throw e;
        } catch (Exception e) {
            throw new CoreException(ErrorType.PAYMENT_PROCESSING_FAILED, e.getMessage());
        }
    }

    private Long generatePaymentId() {
        return System.currentTimeMillis();
    }
}
