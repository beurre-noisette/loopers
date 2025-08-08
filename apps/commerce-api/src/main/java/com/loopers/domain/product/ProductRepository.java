package com.loopers.domain.product;

import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findById(Long id);

    Optional<Product> findByIdWithLock(Long id);

    Product save(Product product);
}
