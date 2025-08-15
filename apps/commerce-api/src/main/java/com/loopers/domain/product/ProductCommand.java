package com.loopers.domain.product;

import java.math.BigDecimal;

public class ProductCommand {

    public record Create(
            String name,
            String description,
            BigDecimal price,
            Integer stock,
            Long brandId
    ) {}
}