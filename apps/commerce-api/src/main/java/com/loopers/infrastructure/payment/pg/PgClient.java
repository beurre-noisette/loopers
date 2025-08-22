package com.loopers.infrastructure.payment.pg;

import com.loopers.infrastructure.payment.pg.config.PgClientConfig;
import com.loopers.interfaces.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "pg-client",
        url = "${pg.api.url:http://localhost:8082}",
        configuration = PgClientConfig.class
)
public interface PgClient {

    @PostMapping("/api/v1/payments")
    ApiResponse<PgPaymentDto.PaymentResponse> requestPayment(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody PgPaymentDto.PaymentRequest request
    );

    @GetMapping("/api/v1/payments/{transactionKey}")
    ApiResponse<PgPaymentDto.TransactionDetailResponse> getTransaction(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable("transactionKey") String transactionKey
    );

    @GetMapping("/api/v1/payments")
    ApiResponse<PgPaymentDto.TransactionDetailResponse> getTransactionByOrderId(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam("orderId") String orderId
    );
}
