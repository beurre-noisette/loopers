package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository brandJpaRepository;

    @Autowired
    public BrandRepositoryImpl(BrandJpaRepository brandJpaRepository) {
        this.brandJpaRepository = brandJpaRepository;
    }

    @Override
    public Brand save(Brand brand) {
        return brandJpaRepository.save(brand);
    }

    @Override
    public boolean existsByName(String brandName) {
        return brandJpaRepository.existsByName(brandName);
    }

    @Override
    public Optional<Brand> findById(Long id) {
        return brandJpaRepository.findById(id);
    }
}
