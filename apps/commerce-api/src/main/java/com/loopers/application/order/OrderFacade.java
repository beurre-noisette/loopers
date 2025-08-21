package com.loopers.application.order;

import com.loopers.application.order.event.OrderCreatedEvent;
import com.loopers.domain.discount.DiscountResult;
import com.loopers.domain.discount.DiscountService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderCommand;
import com.loopers.domain.order.OrderItems;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.StockReservationService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderFacade {

    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final DiscountService discountService;
    private final StockReservationService stockReservationService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public OrderFacade(OrderService orderService, UserService userService,
                       ProductService productService, DiscountService discountService,
                       StockReservationService stockReservationService,
                       ApplicationEventPublisher eventPublisher) {
        this.orderService = orderService;
        this.userService = userService;
        this.productService = productService;
        this.discountService = discountService;
        this.stockReservationService = stockReservationService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderInfo createOrder(String accountId, OrderCommand.Create command) {
        User user = userService.findByAccountId(accountId);

        List<Long> productIds = command.items().stream()
                .map(OrderCommand.CreateItem::productId)
                .toList();
        List<Product> products = productService.findProductsByIds(productIds);

        OrderItems orderItems = OrderItems.create(command.items(), products);

        Order order = orderService.createOrder(user.getId(), orderItems);

        stockReservationService.reserveStock(order.getId(), products, orderItems);

        DiscountResult discount = discountService.calculateDiscount(
                user,
                order,
                command.pointToDiscount(),
                command.userCouponId()
        );

        BigDecimal finalAmount = discount.calculateFinalAmount(order.getTotalAmount());

        order.waitForPayment();

        eventPublisher.publishEvent(new OrderCreatedEvent(
                order.getId(),
                accountId,
                finalAmount,
                command.paymentDetails()
        ));

        return OrderInfo.from(order, discount);
    }

}
