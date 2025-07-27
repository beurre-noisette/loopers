package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrandService {

    private final BrandRepository brandRepository;

    @Autowired
    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional
    public Brand save(BrandCommand.Create command) {
        if (brandRepository.existsByName(command.name())) {
            throw new CoreException(ErrorType.DUPLICATE_VALUE, command.name());
        }

        Brand brand = Brand.of(command);

        return brandRepository.save(brand);
    }

    @Transactional(readOnly = true)
    public Brand findById(Long id) {
        Brand brand = brandRepository.findById(id).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        );

        return brand;
    }
}
