package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment createPaymentFromResult(Long orderId, PaymentMethod method, PaymentResult result) {
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);

        if (existingPayment.isPresent()) {
            log.error("해당 주문에 대한 결제가 이미 존재합니다 - orderId: {}, paymentId: {}, status: {}",
                    orderId, existingPayment.get().getId(), existingPayment.get().getStatus());
            
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, 
                    String.format("해당 주문에 대한 결제가 이미 존재합니다. (상태: %s)", existingPayment.get().getStatus()));
        }

        Payment payment = Payment.create(
                orderId, 
                method, 
                result.amount(), 
                result.transactionKey(), 
                result.status()
        );
        Payment savedPayment = paymentRepository.save(payment);
        
        log.info("결제 생성 완료 - paymentId: {}, orderId: {}, method: {}, amount: {}, transactionKey: {}, status: {}", 
                savedPayment.getId(), orderId, method, result.amount(), result.transactionKey(), result.status());
        
        return savedPayment;
    }

    @Transactional
    public void markPaymentSuccess(Long orderId, String transactionKey) {
        Payment payment = findPaymentByOrderId(orderId);

        payment.markSuccess(transactionKey);

        paymentRepository.save(payment);
    }

    @Transactional
    public void markPaymentFailed(Long orderId, String reason) {
        Payment payment = findPaymentByOrderId(orderId);

        payment.markFailed(reason);

        paymentRepository.save(payment);
    }

    @Transactional
    public void updatePaymentProcessing(Long orderId) {
        Payment payment = findPaymentByOrderId(orderId);

        payment.updateProcessing();

        paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByTransactionKey(String transactionKey) {
        return paymentRepository.findByTransactionKey(transactionKey);
    }

    @Transactional(readOnly = true)
    public List<Payment> findStaleProcessingPayments(ZonedDateTime cutoff) {
        return paymentRepository.findByStatusAndProcessedAtBefore(PaymentStatus.PROCESSING, cutoff);
    }

    @Transactional(readOnly = true)
    public List<Payment> findProcessingPaymentsBetween(ZonedDateTime startTime, ZonedDateTime endTime) {
        return paymentRepository.findByStatusAndCreatedAtBetween(PaymentStatus.PROCESSING, startTime, endTime);
    }

    private Payment findPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, 
                        "해당 주문의 결제 정보를 찾을 수 없습니다. orderId: " + orderId));
    }
}
