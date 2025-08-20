package com.loopers.infrastructure.payment.pg;

import com.loopers.interfaces.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "pg-client",
        url = "${pg.api.url:http://localhost:8082}"
)
@RequestMapping("/api/v1/payments")
public interface PgClient {

    @PostMapping
    ApiResponse<PgPaymentDto.PaymentResponse> requestPayment(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody PgPaymentDto.PaymentRequest request
    );

    @GetMapping("/{transactionKey}")
    ApiResponse<PgPaymentDto.TransactionDetailResponse> getTransaction(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable("transactionKey") String transactionKey
    );

    @GetMapping
    ApiResponse<PgPaymentDto.TransactionDetailResponse> getTransactionByOrderId(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam("orderId") String orderId
    );
}
