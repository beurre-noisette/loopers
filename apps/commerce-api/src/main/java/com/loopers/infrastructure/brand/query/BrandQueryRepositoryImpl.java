package com.loopers.infrastructure.brand.query;

import com.loopers.application.brand.BrandQueryRepository;
import com.loopers.domain.brand.QBrand;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class BrandQueryRepositoryImpl implements BrandQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Autowired
    public BrandQueryRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public Optional<BrandQueryData> findBrandInfoById(Long brandId) {
        QBrand brand = QBrand.brand;

        BrandQueryData result = jpaQueryFactory
                .select(Projections.constructor(BrandQueryData.class,
                        brand.id,
                        brand.name,
                        brand.description))
                .from(brand)
                .where(brand.id.eq(brandId)
                        .and(brand.deletedAt.isNull()))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
