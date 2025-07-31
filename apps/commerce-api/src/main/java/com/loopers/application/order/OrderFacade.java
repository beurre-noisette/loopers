package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderCommand;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
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

    @Autowired
    public OrderFacade(OrderService orderService, UserService userService, ProductService productService) {
        this.orderService = orderService;
        this.userService = userService;
        this.productService = productService;
    }

    @Transactional
    public OrderInfo createOrder(String userId, OrderCommand.Create command) {
        User user = userService.findByUserId(userId);
        
        List<Product> products = productService.findProductsForOrder(command.items());
        
        List<OrderItem> orderItems = orderService.createOrderItems(command.items(), products);
        
        BigDecimal totalAmount = orderService.calculateTotalAmount(orderItems);
        
        productService.validateAndDecreaseStocks(products, command.items());
        
        userService.usePoint(user, totalAmount.intValue());
        
        Order order = orderService.createOrder(userId, orderItems);
        
        scheduleExternalNotification(order);
        
        return OrderInfo.from(order);
    }

    private void scheduleExternalNotification(Order order) {
        // TODO: 외부 시스템 알림 구현
        // 현재는 로그만 출력
        System.out.println("주문 알림: 주문 ID " + order.getId() + " 생성됨");
    }
}
