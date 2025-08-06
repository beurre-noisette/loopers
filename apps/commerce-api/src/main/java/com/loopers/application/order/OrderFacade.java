package com.loopers.application.order;

import com.loopers.domain.discount.DiscountResult;
import com.loopers.domain.discount.DiscountService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderCommand;
import com.loopers.domain.order.OrderItems;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.PaymentReference;
import com.loopers.domain.payment.PaymentResult;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.StockManagementService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderFacade {

    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final StockManagementService stockManagementService;
    private final DiscountService discountService;
    private final PaymentService paymentService;
    private final PointService pointService;

    @Autowired
    public OrderFacade(OrderService orderService, UserService userService,
                       ProductService productService, StockManagementService stockManagementService, DiscountService discountService, PaymentService paymentService, PointService pointService) {
        this.orderService = orderService;
        this.userService = userService;
        this.productService = productService;
        this.stockManagementService = stockManagementService;
        this.discountService = discountService;
        this.paymentService = paymentService;
        this.pointService = pointService;
    }

    @Transactional
    public OrderInfo createOrder(String userId, OrderCommand.Create command) {
        User user = userService.findByUserId(userId);

        List<Long> productIds = command.items().stream()
                .map(OrderCommand.CreateItem::productId)
                .toList();
        List<Product> products = productService.findProductsByIds(productIds);

        OrderItems orderItems = OrderItems.create(command.items(), products);

        stockManagementService.decreaseStock(products, orderItems);

        Order order = orderService.createOrder(userId, orderItems);

        DiscountResult discount = discountService.calculateDiscount(order, command.pointToDiscount());

        BigDecimal finalAmount = order.getTotalAmount().subtract(discount.getTotalDiscount());

        pointService.usePointForDiscount(user.getId(), command.pointToDiscount(), order.getId());

        PaymentResult payment = paymentService.processPayment(
                user.getId(),
                finalAmount,
                PaymentReference.order(order.getId())
        );

        order.complete();

        scheduleExternalNotification(order);

        return OrderInfo.from(order, payment, command.pointToDiscount());
    }

    private void scheduleExternalNotification(Order order) {
        // TODO: 외부 시스템 알림 구현
        // 현재는 로그만 출력
        System.out.println("주문 알림: 주문 ID " + order.getId() + " 생성됨");
    }
}
