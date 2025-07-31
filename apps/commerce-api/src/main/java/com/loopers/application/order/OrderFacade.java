package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderCommand;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
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
        List<Product> products = getProducts(command.items());

        List<OrderItem> orderItems = createOrderItems(command.items(), products);

        productService.decreaseStock(products, command.items());
        
        BigDecimal totalAmount = calculateTotalAmount(orderItems);
        userService.usePoint(user, totalAmount.intValue());

        Order order = orderService.createOrder(userId, orderItems);

        scheduleExternalNotification(order);

        return OrderInfo.from(order);
    }

    private List<Product> getProducts(List<OrderCommand.CreateItem> items) {
        return items.stream()
                .map(item -> productService.findById(item.productId()))
                .toList();
    }


    private List<OrderItem> createOrderItems(List<OrderCommand.CreateItem> items, List<Product> products) {
        return items.stream()
                .map(item -> {
                    Product product = findProductById(products, item.productId());
                    return new OrderItem(
                            item.productId(),
                            item.quantity(),
                            product.getPrice()  // 주문 시점의 가격으로 OrderItem 생성
                    );
                })
                .toList();
    }

    private Product findProductById(List<Product> products, Long productId) {
        return products.stream()
                .filter(product -> product.getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    private BigDecimal calculateTotalAmount(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void scheduleExternalNotification(Order order) {
        // TODO: 외부 시스템 알림 구현
        // 현재는 로그만 출력
        System.out.println("주문 알림: 주문 ID " + order.getId() + " 생성됨");
    }
}
