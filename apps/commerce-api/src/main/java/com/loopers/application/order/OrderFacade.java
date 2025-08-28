package com.loopers.application.order;

import com.loopers.application.order.event.OrderCreatedEvent;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderCommand;
import com.loopers.domain.order.OrderItems;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.StockReservationService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class OrderFacade {

    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final StockReservationService stockReservationService;
    private final CouponService couponService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public OrderFacade(OrderService orderService, UserService userService,
                       ProductService productService,
                       StockReservationService stockReservationService,
                       CouponService couponService,
                       ApplicationEventPublisher eventPublisher) {
        this.orderService = orderService;
        this.userService = userService;
        this.productService = productService;
        this.stockReservationService = stockReservationService;
        this.couponService = couponService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderInfo createOrder(String accountId, OrderCommand.Create command) {
        log.info("주문 생성 시작 - accountId: {}, items: {}", accountId, command.items().size());
        
        User user = userService.findByAccountId(accountId);

        List<Long> productIds = command.items().stream()
                .map(OrderCommand.CreateItem::productId)
                .toList();
        List<Product> products = productService.findProductsByIds(productIds);

        OrderItems orderItems = OrderItems.create(command.items(), products);
        
        Order order = orderService.createOrder(user.getId(), orderItems);
        
        stockReservationService.reserveStock(order.getId(), products, orderItems);
        
        BigDecimal couponDiscountAmount = couponService.calculateCouponDiscount(
                command.userCouponId(), 
                order.getTotalAmount()
        );
        
        order.applyDiscount(couponDiscountAmount);
        
        order.waitForPayment();
        
        OrderCreatedEvent event = OrderCreatedEvent.of(
                order.getId(),
                user.getId(),
                command.userCouponId(),
                command.paymentDetails()
        );
        
        eventPublisher.publishEvent(event);
        
        log.info("주문 생성 완료 - orderId: {}, correlationId: {}, finalAmount: {}",
                order.getId(), event.getCorrelationId(), order.getTotalAmount());
        
        return OrderInfo.from(order);
    }
    

}
