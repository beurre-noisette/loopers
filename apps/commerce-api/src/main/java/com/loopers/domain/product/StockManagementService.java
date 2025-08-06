package com.loopers.domain.product;

import com.loopers.domain.order.OrderItems;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class StockManagementService {

    private final ProductRepository productRepository;

    @Autowired
    public StockManagementService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public void decreaseStock(List<Product> products, OrderItems orderItems) {
        Map<Long, Integer> requiredQuantities = orderItems.getProductQuantityMap();

        for (Product product : products) {
            Integer quantity = requiredQuantities.get(product.getId());
            if (quantity != null) {
                Product lockedProduct = productRepository.findByIdWithLock(product.getId())
                        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

                lockedProduct.decreaseStock(quantity);
                productRepository.save(lockedProduct);
            }
        }
    }
}
