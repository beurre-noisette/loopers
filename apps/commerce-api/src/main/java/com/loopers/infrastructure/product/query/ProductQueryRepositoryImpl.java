package com.loopers.infrastructure.product.query;

import com.loopers.application.product.ProductQueryRepository;
import com.loopers.application.product.ProductSortType;
import com.loopers.domain.brand.QBrand;
import com.loopers.domain.product.QProduct;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductQueryRepositoryImpl implements ProductQueryRepository {
    
    private final JPAQueryFactory jpaQueryFactory;
    
    @Autowired
    public ProductQueryRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }
    
    @Override
    public Page<ProductQueryData> findProducts(Long brandId, ProductSortType sortType, Pageable pageable) {
        QProduct product = QProduct.product;
        QBrand brand = QBrand.brand;
        
        List<ProductQueryData> content = jpaQueryFactory
            .select(Projections.constructor(ProductQueryData.class,
                product.id,
                product.name,
                product.description,
                product.price,
                product.stock,
                brand.id,
                brand.name,
                Expressions.asNumber(0L) // FIXME 좋아요 수는 일단 0으로 (추후 Like 엔티티 연동 시 수정)
            ))
            .from(product)
            .join(product.brand, brand)
            .where(
                product.deletedAt.isNull(),
                brand.deletedAt.isNull(),
                brandIdEq(brandId)
            )
            .orderBy(getOrderSpecifier(sortType, product))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
        
        Long total = jpaQueryFactory
            .select(product.count())
            .from(product)
            .join(product.brand, brand)
            .where(
                product.deletedAt.isNull(),
                brand.deletedAt.isNull(),
                brandIdEq(brandId)
            )
            .fetchOne();
        
        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
    
    @Override
    public Optional<ProductDetailQueryData> findProductDetailById(Long productId) {
        QProduct product = QProduct.product;
        QBrand brand = QBrand.brand;
        
        ProductDetailQueryData result = jpaQueryFactory
            .select(Projections.constructor(ProductDetailQueryData.class,
                product.id,
                product.name,
                product.description,
                product.price,
                product.stock,
                brand.id,
                brand.name,
                brand.description,
                Expressions.asNumber(0L) // FIXME 좋아요 수는 일단 0으로 (추후 Like 엔티티 연동 시 수정)
            ))
            .from(product)
            .join(product.brand, brand)
            .where(
                product.id.eq(productId),
                product.deletedAt.isNull(),
                brand.deletedAt.isNull()
            )
            .fetchOne();
        
        return Optional.ofNullable(result);
    }
    
    private BooleanExpression brandIdEq(Long brandId) {
        return brandId != null ? QProduct.product.brand.id.eq(brandId) : null;
    }
    
    private OrderSpecifier<?> getOrderSpecifier(ProductSortType sortType, QProduct product) {
        return switch (sortType) {
            case LATEST -> product.createdAt.desc();
            case PRICE_ASC -> product.price.asc();
            case LIKES_DESC -> product.createdAt.desc(); // FIXME 좋아요 수 정렬은 추후 구현
        };
    }
}
