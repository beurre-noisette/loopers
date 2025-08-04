package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }


    @Transactional
    public Order createOrder(String userId, OrderItems orderItems) {
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
}
