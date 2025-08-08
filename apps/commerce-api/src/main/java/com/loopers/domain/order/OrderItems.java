package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OrderItems {
    private final List<OrderItem> items;

    private OrderItems(List<OrderItem> items) {
        validateNotEmpty(items);
        this.items = new ArrayList<>(items);
    }

    public static OrderItems from(List<OrderItem> items) {
        return new OrderItems(items);
    }

    public BigDecimal calculateTotalAmount() {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<Long, Integer> getProductQuantityMap() {
        return items.stream()
                .collect(Collectors.toMap(
                        OrderItem::productId,
                        OrderItem::quantity
                ));
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int size() {
        return items.size();
    }

    public static OrderItems create(List<OrderCommand.CreateItem> commandItems, List<Product> products) {
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        
        List<OrderItem> orderItemList = commandItems.stream()
                .map(item -> {
                    Product product = productMap.get(item.productId());
                    if (product == null) {
                        throw new CoreException(ErrorType.NOT_FOUND, 
                                "상품을 찾을 수 없습니다. ID: " + item.productId());
                    }
                    return new OrderItem(
                            item.productId(),
                            item.quantity(),
                            product.getPrice()
                    );
                })
                .toList();
        
        return OrderItems.from(orderItemList);
    }

    private static void validateNotEmpty(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, 
                    "주문 항목은 최소 1개 이상이어야 합니다.");
        }
    }
}