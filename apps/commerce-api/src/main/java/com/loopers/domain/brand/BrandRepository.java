package com.loopers.domain.brand;

import java.util.Optional;

public interface BrandRepository {

    Brand save(Brand brand);

    boolean existsByName(String brandName);

    Optional<Brand> findById(Long id);
}
