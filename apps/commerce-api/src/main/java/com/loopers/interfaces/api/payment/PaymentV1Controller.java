package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
public class PaymentV1Controller {

    private final PaymentFacade paymentFacade;

    @Autowired
    public PaymentV1Controller(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    @PostMapping("/callback")
    public ApiResponse<PaymentV1Dto.CallbackResponse> handlePaymentCallback(
            @RequestBody PaymentV1Dto.CallbackRequest request
    ) {
        log.info("PG 콜백 수신 - transactionKey: {}, orderId: {}, status: {}", 
                request.transactionKey(), request.orderId(), request.status());
        
        try {
            paymentFacade.handlePaymentCallback(
                    request.transactionKey(),
                    request.orderId(),
                    request.status(),
                    request.reason()
            );
            
            return ApiResponse.success(
                new PaymentV1Dto.CallbackResponse("OK", "콜백 처리 완료")
            );
            
        } catch (Exception e) {
            log.error("PG 콜백 처리 중 오류 발생 - transactionKey: {}", request.transactionKey(), e);
            
            return ApiResponse.success(
                new PaymentV1Dto.CallbackResponse("ERROR", "콜백 처리 실패: " + e.getMessage())
            );
        }
    }
}
