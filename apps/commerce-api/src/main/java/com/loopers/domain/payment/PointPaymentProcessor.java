package com.loopers.domain.payment;

import com.loopers.domain.point.PointReference;
import com.loopers.domain.point.PointService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Service("pointPaymentProcessor")
@Slf4j
public class PointPaymentProcessor implements PaymentProcessor {

    private final PointService pointService;

    @Autowired
    public PointPaymentProcessor(PointService pointService) {
        this.pointService = pointService;
    }

    @Override
    public void validatePaymentCapability(Long userId, PaymentCommand command) {
        if (command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.PAYMENT_VALIDATION_FAILED, "결제 금액은 0보다 커야 합니다.");
        }
    }

    @Override
    public PaymentResult processPayment(Long userId, PaymentCommand command) {
        try {
            validatePaymentCapability(userId, command);

            PointReference pointReference = PointReference.order(command.getOrderId());
            
            log.info("포인트 결제 시도 - userId: {}, orderId: {}, amount: {}", 
                    userId, command.getOrderId(), command.getAmount());

            pointService.usePoint(userId, command.getAmount(), pointReference);

            log.info("포인트 결제 성공 - userId: {}, orderId: {}, amount: {}", 
                    userId, command.getOrderId(), command.getAmount());
            
            return new PaymentResult(
                    null,
                    command.getAmount(),
                    PaymentStatus.SUCCESS,
                    ZonedDateTime.now(),
                    "포인트 결제 성공",
                    "POINT_" + userId
            );

        } catch (CoreException e) {
            String failureReason;
            if (e.getErrorType() == ErrorType.NOT_ENOUGH) {
                failureReason = "포인트 잔액 부족";
            } else {
                failureReason = e.getMessage();
            }
            
            return new PaymentResult(
                    null,
                    command.getAmount(),
                    PaymentStatus.FAILED,
                    ZonedDateTime.now(),
                    failureReason,
                    null
            );
            
        } catch (Exception e) {
            return new PaymentResult(
                    null,
                    command.getAmount(),
                    PaymentStatus.FAILED,
                    ZonedDateTime.now(),
                    "결제 처리 중 오류: " + e.getMessage(),
                    null
            );
        }
    }
}
