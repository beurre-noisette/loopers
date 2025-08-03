package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderItem> createOrderItems(List<OrderCommand.CreateItem> items, List<Product> products) {
        return items.stream()
                .map(item -> {
                    Product product = findProductById(products, item.productId());
                    return new OrderItem(
                            item.productId(),
                            item.quantity(),
                            product.getPrice()
                    );
                })
                .toList();
    }

    public BigDecimal calculateTotalAmount(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Order createOrder(String userId, List<OrderItem> orderItems) {
        Order order = Order.create(userId, orderItems);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order findById(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        );
    }

    @Transactional(readOnly = true)
    public List<Order> findByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }

    private Product findProductById(List<Product> products, Long productId) {
        return products.stream()
                .filter(product -> product.getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }
}
