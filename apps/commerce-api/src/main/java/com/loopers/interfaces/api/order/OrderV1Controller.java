package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.domain.order.OrderCommand;
import com.loopers.domain.payment.PaymentDetails;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @Autowired
    public OrderV1Controller(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }

    @PostMapping
    public ApiResponse<OrderV1Dto.OrderCreateResponse> createOrder(
            @RequestHeader("X-USER-ID") String accountId,
            @RequestBody OrderV1Dto.OrderCreateRequest request
    ) {
        List<OrderCommand.CreateItem> commandItems = request.items().stream()
            .map(item -> new OrderCommand.CreateItem(item.productId(), item.quantity()))
            .toList();

        PaymentDetails paymentDetails = createPaymentDetails(request.paymentMethod(), request.cardInfo());

        OrderCommand.Create command = new OrderCommand.Create(
            commandItems,
            request.pointToDiscount(),
            request.userCouponId(),
            paymentDetails
        );

        OrderInfo orderInfo = orderFacade.createOrder(accountId, command);
        OrderV1Dto.OrderCreateResponse response = OrderV1Dto.OrderCreateResponse.from(orderInfo);

        return ApiResponse.success(response);
    }
    
    private PaymentDetails createPaymentDetails(PaymentMethod paymentMethod, OrderV1Dto.CardInfoRequest cardInfo) {
        return switch (paymentMethod) {
            case CARD -> {
                if (cardInfo == null) {
                    throw new CoreException(ErrorType.BAD_REQUEST, "카드 결제 시 카드 정보는 필수입니다.");
                }
                yield new PaymentDetails.Card(cardInfo.cardType(), cardInfo.cardNo());
            }
            case POINT -> new PaymentDetails.Point();
        };
    }

}
