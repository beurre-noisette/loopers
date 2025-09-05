package com.loopers.domain.product;

import com.loopers.domain.order.OrderItems;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        List<Product> sortedProducts = products.stream()
                .sorted(Comparator.comparing(Product::getId))
                .toList();

        for (Product product : sortedProducts) {
            Integer quantity = requiredQuantities.get(product.getId());
            if (quantity != null) {
                Product lockedProduct = productRepository.findByIdWithLock(product.getId())
                        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

                lockedProduct.decreaseStock(quantity);
                productRepository.save(lockedProduct);
            }
        }
    }

    @Transactional
    public List<StockReservationResult> increaseStock(List<StockReservation> reservations) {
        Map<Long, Integer> productQuantityMap = reservations.stream()
                .collect(Collectors.groupingBy(
                        StockReservation::getProductId,
                        Collectors.summingInt(StockReservation::getQuantity)
                ));

        List<Long> productIds = List.copyOf(productQuantityMap.keySet());
        List<Long> sortedProductIds = productIds.stream()
                .sorted()
                .toList();

        List<StockReservationResult> stockReservationResults = new ArrayList<>();
        
        for (Long productId : sortedProductIds) {
            Integer quantity = productQuantityMap.get(productId);
            
            Product lockedProduct = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

            lockedProduct.increaseStock(quantity);
            productRepository.save(lockedProduct);
            
            stockReservationResults.add(StockReservationResult.of(
                    lockedProduct.getId(),
                    quantity,
                    lockedProduct.getStock()
            ));
        }
        
        return stockReservationResults;
    }
}
