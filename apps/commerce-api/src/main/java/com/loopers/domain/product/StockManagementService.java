package com.loopers.domain.product;

import com.loopers.domain.order.OrderItems;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StockManagementService {

    public void validateStock(List<Product> products, OrderItems orderItems) {
        Map<Long, Integer> requiredQuantities = orderItems.getProductQuantityMap();
        
        for (Product product : products) {
            Integer requiredQuantity = requiredQuantities.get(product.getId());
            if (requiredQuantity != null && product.getStock() < requiredQuantity) {
                throw new CoreException(ErrorType.INVALID_INPUT_FORMAT,
                        String.format("재고가 부족합니다. 상품: %s, 현재 재고: %d, 요청 수량: %d",
                                product.getName(), product.getStock(), requiredQuantity));
            }
        }
    }

    public void decreaseStock(List<Product> products, OrderItems orderItems) {
        Map<Long, Integer> requiredQuantities = orderItems.getProductQuantityMap();
        
        products.forEach(product -> {
            Integer quantity = requiredQuantities.get(product.getId());
            if (quantity != null) {
                product.decreaseStock(quantity);
            }
        });
    }
}
