package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.*;
import com.loopers.domain.product.*;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
public class PaymentFacade {

    private final OrderService orderService;
    private final PaymentServiceFactory paymentServiceFactory;
    private final StockReservationService stockReservationService;
    private final UserService userService;
    private final ProductService  productService;

    @Autowired
    public PaymentFacade(
            OrderService orderService,
            PaymentServiceFactory paymentServiceFactory,
            StockReservationService stockReservationService,
            UserService userService,
            ProductService productService
    ) {
        this.orderService = orderService;
        this.paymentServiceFactory = paymentServiceFactory;
        this.stockReservationService = stockReservationService;
        this.userService = userService;
        this.productService = productService;
    }

    @Transactional
    public PaymentInfo.ProcessResponse processPayment(String userIdStr, PaymentCommand.ProcessPayment command) {
        User user = userService.findByUserId(userIdStr);

        Order order = orderService.findById(command.orderId());

        validateOrderForPayment(order, user.getId());

        PaymentService paymentService = paymentServiceFactory.getPaymentService(command.method());

        PaymentReference reference = (command.method() == PaymentMethod.CARD)
                ? PaymentReference.orderWithCard(order.getId(), command.cardInfo())
                : PaymentReference.order(order.getId());

        PaymentResult result = paymentService.processPayment(
                user.getId(),
                order.getTotalAmount(),
                reference
        );

        handlePaymentResult(order, result, command.method());

        return PaymentInfo.ProcessResponse.from(command.orderId(), command.method(), result);
    }

    private void validateOrderForPayment(Order order, Long userId) {
        if (!order.getUserId().equals(userId)) {
            throw new CoreException(ErrorType.FORBIDDEN, "본인의 주문건만 결제할 수 있습니다.");
        }

        if (!order.isPaymentWaiting()) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT,
                    "결제 대기 상태의 주문만 결제할 수 있습니다. 현재 상태: " + order.getStatus());
        }
    }

    private void handlePaymentResult(Order order, PaymentResult result, PaymentMethod method) {
        switch (result.status()) {
            case SUCCESS -> completeOrder(order);
            case PROCESSING -> order.processingPayment();
            case FAILED -> log.error("결제 실패 - orderId: {}, message: {}", order.getId(), result.message());
        }
    }

    private void completeOrder(Order order) {
        stockReservationService.confirmReservation(order.getId());

        var reservations = stockReservationService.getReservations(order.getId());
        List<Long> productIds = reservations.stream()
                .map(StockReservation::getProductId)
                .toList();
        List<Product> products = productService.findProductsByIds(productIds);

        reservations.forEach(reservation -> {
            Product product = Product.findById(products, reservation.getProductId());
            product.decreaseStock(reservation.getQuantity());
        });

        order.completePayment();
    }
}
