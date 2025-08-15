package com.loopers.infrastructure.product.query;

import com.loopers.application.product.ProductQueryRepository;
import com.loopers.application.product.ProductSortType;
import com.loopers.domain.like.TargetType;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.loopers.domain.brand.QBrand.brand;
import static com.loopers.domain.like.QLike.like;
import static com.loopers.domain.product.QProduct.product;
import static com.querydsl.jpa.JPAExpressions.select;

@Repository
public class ProductQueryRepositoryImpl implements ProductQueryRepository {
    
    private final JPAQueryFactory jpaQueryFactory;

    @Autowired
    public ProductQueryRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }
    
    @Override
    public Page<ProductQueryData> findProducts(Long brandId, ProductSortType sortType, Pageable pageable) {

        List<ProductQueryData> content = jpaQueryFactory
            .select(Projections.constructor(ProductQueryData.class,
                product.id,
                product.name,
                product.description,
                product.price,
                product.stock,
                brand.id,
                brand.name,
                product.likeCount
            ))
            .from(product)
            .join(product.brand, brand)
            .where(
                product.deletedAt.isNull(),
                brand.deletedAt.isNull(),
                brandIdEq(brandId)
            )
            .orderBy(getOrderSpecifier(sortType))
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
                product.likeCount
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

    private static BooleanExpression brandIdEq(Long brandId) {
        return brandId != null ? product.brand.id.eq(brandId) : null;
    }

    private OrderSpecifier<?> getOrderSpecifier(ProductSortType sortType) {
        return switch (sortType) {
            case LATEST -> product.createdAt.desc();
            case PRICE_ASC -> product.price.asc();
            case LIKES_DESC -> {
                var likeCountSubquery = select(like.count())
                    .from(like)
                    .where(
                        like.targetType.eq(TargetType.PRODUCT),
                        like.targetId.eq(product.id)
                    );
                
                yield new OrderSpecifier<>(Order.DESC, likeCountSubquery);
            }
        };
    }
}
