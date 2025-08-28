package com.loopers.application.like.event;

import com.loopers.application.product.ProductQuery;
import com.loopers.domain.like.TargetType;
import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("좋아요 집계 이벤트 리스너 테스트")
class LikeAggregateListenerTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductQuery productQuery;

    @InjectMocks
    private LikeAggregateListener likeAggregateListener;

    @DisplayName("좋아요 생성 이벤트 처리 시")
    @Nested
    class HandleLikeCreated {

        @DisplayName("상품 좋아요 수 증가와 캐시 무효화를 수행한다")
        @Test
        void increasesProductLikeCountAndEvictsCache() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            LikeCreatedEvent event = LikeCreatedEvent.of(userId, TargetType.PRODUCT, productId);

            // act
            likeAggregateListener.handleLikeCreated(event);

            // assert
            verify(productService).increaseLikeCount(productId);
            verify(productQuery).evictProductDetailCache(productId);
        }

        @DisplayName("ProductService 예외 발생 시 조용히 처리한다")
        @Test
        void handlesProductServiceExceptionSilently() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            LikeCreatedEvent event = LikeCreatedEvent.of(userId, TargetType.PRODUCT, productId);

            doThrow(new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."))
                    .when(productService).increaseLikeCount(productId);

            // act & assert (예외가 전파되지 않아야 함)
            likeAggregateListener.handleLikeCreated(event);

            verify(productService).increaseLikeCount(productId);
            // 캐시 무효화는 호출되지 않아야 함 (예외로 인해 중단)
            verifyNoInteractions(productQuery);
        }
    }

    @DisplayName("좋아요 취소 이벤트 처리 시")
    @Nested
    class HandleLikeCancelled {

        @DisplayName("상품 좋아요 수 감소와 캐시 무효화를 수행한다")
        @Test
        void decreasesProductLikeCountAndEvictsCache() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            LikeCancelledEvent event = LikeCancelledEvent.of(userId, TargetType.PRODUCT, productId);

            // act
            likeAggregateListener.handleLikeCancelled(event);

            // assert
            verify(productService).decreaseLikeCount(productId);
            verify(productQuery).evictProductDetailCache(productId);
        }

        @DisplayName("ProductService 예외 발생 시 조용히 처리한다")
        @Test
        void handlesProductServiceExceptionSilently() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            LikeCancelledEvent event = LikeCancelledEvent.of(userId, TargetType.PRODUCT, productId);

            doThrow(new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."))
                    .when(productService).decreaseLikeCount(productId);

            // act & assert (예외가 전파되지 않아야 함)
            likeAggregateListener.handleLikeCancelled(event);

            verify(productService).decreaseLikeCount(productId);
            // 캐시 무효화는 호출되지 않아야 함 (예외로 인해 중단)
            verifyNoInteractions(productQuery);
        }
    }
}
