package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.domain.order.OrderCommand;
import com.loopers.interfaces.api.ApiResponse;
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
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody OrderV1Dto.OrderCreateRequest request
    ) {
        List<OrderCommand.CreateItem> commandItems = request.items().stream()
            .map(item -> new OrderCommand.CreateItem(item.productId(), item.quantity()))
            .toList();

        OrderCommand.Create.CardInfo cardInfo = null;
        if (request.cardInfo() != null) {
            cardInfo = new OrderCommand.Create.CardInfo(
                request.cardInfo().cardType(),
                request.cardInfo().cardNo()
            );
        }

        OrderCommand.Create command = new OrderCommand.Create(
            commandItems,
            request.pointToDiscount(),
            request.userCouponId(),
            request.paymentMethod(),
            cardInfo
        );

        OrderInfo orderInfo = orderFacade.createOrder(userId, command);
        OrderV1Dto.OrderCreateResponse response = OrderV1Dto.OrderCreateResponse.from(orderInfo);

        return ApiResponse.success(response);
    }

}
