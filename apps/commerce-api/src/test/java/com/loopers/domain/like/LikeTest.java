package com.loopers.domain.like;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LikeTest {

    @DisplayName("좋아요 생성 시")
    @Nested
    class Create {
        @DisplayName("User와 Target이 주어지면 좋아요가 생성된다.")
        @Test
        void createLike_whenProvidedUserAndTarget() {
            // arrange
            User user = createTestUser();
            Target target = ProductTarget.of(1L);

            // act
            Like like = Like.of(user, target);

            // assert
            assertThat(like.getUser()).isEqualTo(user);
            assertThat(like.getTargetType()).isEqualTo(TargetType.PRODUCT);
            assertThat(like.getTargetId()).isEqualTo(1L);
            assertThat(like.getUserId()).isEqualTo(user.getId());
        }

        @DisplayName("User가 null이면 BAD REQUEST 예외가 발생한다")
        @Test
        void throwBadRequestException_whenUserIsNull() {
            // arrange
            Target target = ProductTarget.of(1L);

            // act
            CoreException exception = assertThrows(CoreException.class,
                    () -> Like.of(null, target));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("Target이 null이면 BAD REQUEST 예외가 발생한다")
        @Test
        void throwBadRequestException_whenTargetIsNull() {
            // arrange
            User user = createTestUser();

            // act
            CoreException exception = assertThrows(CoreException.class,
                    () -> Like.of(user, null));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("좋아요 동작")
    @Nested
    class LikeBehavior {

        @DisplayName("상품에 대해서")
        @Nested
        class Product {
            @DisplayName("상품 좋아요 정보를 올바르게 저장한다")
            @Test
            void storeProductLikeInfo() {
                // arrange
                User user = createTestUser();
                Long productId = 100L;
                ProductTarget productTarget = ProductTarget.of(productId);

                // act
                Like like = Like.of(user, productTarget);

                // assert
                assertThat(like.getTargetType()).isEqualTo(TargetType.PRODUCT);
                assertThat(like.getTargetId()).isEqualTo(productId);
                assertThat(like.getUserId()).isEqualTo(user.getId());
            }

            @DisplayName("저장된 상품 정보를 Target 객체로 복원할 수 있다")
            @Test
            void restoreProductTarget() {
                // arrange
                User user = createTestUser();
                Long productId = 100L;
                Like like = Like.of(user, ProductTarget.of(productId));

                // act
                Target restoredTarget = like.getTarget();
                ProductTarget productTarget = (ProductTarget) restoredTarget;

                // assert
                assertThat(restoredTarget).isInstanceOf(ProductTarget.class);
                assertThat(restoredTarget.getType()).isEqualTo(TargetType.PRODUCT);
                assertThat(restoredTarget.getId()).isEqualTo(productId);
                assertThat(productTarget.getProductId()).isEqualTo(productId);
            }

            @DisplayName("특정 상품에 대한 좋아요인지 확인할 수 있다")
            @Test
            void checkIfLikeForSpecificProduct() {
                // arrange
                User user = createTestUser();
                Long targetProductId = 1L;
                Long otherProductId = 2L;

                // act
                Like like = Like.of(user, ProductTarget.of(targetProductId));

                // assert
                assertThat(like.isTarget(ProductTarget.of(targetProductId))).isTrue();
                assertThat(like.isTarget(ProductTarget.of(otherProductId))).isFalse();
                assertThat(like.isTargetType(TargetType.PRODUCT)).isTrue();
            }
        }
    }

    private User createTestUser() {
        UserCommand.Create command = new UserCommand.Create(
                "testUser",
                "test@example.com",
                "1990-01-01",
                Gender.MALE
        );
        return User.of(command);
    }
}
