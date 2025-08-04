package com.loopers.application.order;

import com.loopers.domain.order.*;
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

    @Autowired
    public OrderFacade(OrderService orderService, UserService userService, 
                      ProductService productService, StockManagementService stockManagementService) {
        this.orderService = orderService;
        this.userService = userService;
        this.productService = productService;
        this.stockManagementService = stockManagementService;
    }

    @Transactional
    public OrderInfo createOrder(String userId, OrderCommand.Create command) {
        User user = userService.findByUserId(userId);
        
        List<Long> productIds = command.items().stream()
                .map(OrderCommand.CreateItem::productId)
                .toList();
        List<Product> products = productService.findProductsByIds(productIds);
        
        OrderItems orderItems = OrderItems.create(command.items(), products);
        
        stockManagementService.validateStock(products, orderItems);
        stockManagementService.decreaseStock(products, orderItems);
        
        BigDecimal totalAmount = orderItems.calculateTotalAmount();
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
