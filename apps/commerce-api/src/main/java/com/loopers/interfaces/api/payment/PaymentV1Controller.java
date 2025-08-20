package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.domain.payment.PaymentCommand;
import com.loopers.interfaces.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentV1Controller {

    private final PaymentFacade paymentFacade;

    @Autowired
    public PaymentV1Controller(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    @PostMapping("/process")
    public ApiResponse<PaymentInfo.ProcessResponse> processPayment(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody PaymentV1Dto.ProcessRequest request
    ) {
        PaymentCommand.ProcessPayment command = request.toCommand();

        PaymentInfo.ProcessResponse response = paymentFacade.processPayment(
                userId,
                command
        );

        return ApiResponse.success(response);
    }
}
