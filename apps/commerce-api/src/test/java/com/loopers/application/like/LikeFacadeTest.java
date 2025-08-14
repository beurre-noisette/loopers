package com.loopers.application.like;

import com.loopers.domain.like.LikeCommand;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.like.ProductTarget;
import com.loopers.domain.like.TargetType;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeFacadeTest {

    @Mock
    private UserService userService;

    @Mock
    private LikeService likeService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private LikeFacade likeFacade;

    @DisplayName("좋아요 등록 시")
    @Nested
    class CreateLike {

        @DisplayName("Command를 받아 좋아요 등록에 성공한다.")
        @Test
        void createLike_success() {
            // arrange
            String userId = "testUser";
            Long productId = 1L;
            LikeCommand.Create command = new LikeCommand.Create(userId, TargetType.PRODUCT, productId);

            User user = createTestUser();
            when(userService.findByUserId(userId)).thenReturn(user);
            when(likeService.createLike(eq(user), any(ProductTarget.class))).thenReturn(true);

            // act
            likeFacade.createLike(command);

            // assert
            verify(userService).findByUserId(userId);
            verify(likeService).createLike(eq(user), any(ProductTarget.class));
            verify(productService).increaseLikeCount(command.targetId());
        }

        @DisplayName("ProductTarget이 올바르게 생성된다.")
        @Test
        void createLike_createCorrectTarget() {
            // arrange
            String userId = "testUser";
            Long productId = 100L;
            LikeCommand.Create command = new LikeCommand.Create(userId, TargetType.PRODUCT, productId);

            User user = createTestUser();
            when(userService.findByUserId(userId)).thenReturn(user);

            // act
            likeFacade.createLike(command);

            // assert
            verify(likeService).createLike(eq(user), argThat(target ->
                            target instanceof ProductTarget &&
                            target.getId().equals(productId) &&
                            target.getType() == TargetType.PRODUCT
            ));
        }
    }

    @DisplayName("좋아요 취소 시")
    @Nested
    class CancelLike {

        @DisplayName("Command를 받아 좋아요 취소가 성공한다")
        @Test
        void cancelLike_success() {
            // arrange
            String userId = "testUser";
            Long productId = 1L;
            LikeCommand.Create command = new LikeCommand.Create(userId, TargetType.PRODUCT, productId);

            User user = createTestUser();
            when(userService.findByUserId(userId)).thenReturn(user);
            when(likeService.cancelLike(eq(user), any(ProductTarget.class))).thenReturn(true);

            // act
            likeFacade.cancelLike(command);

            // assert
            verify(userService).findByUserId(userId);
            verify(likeService).cancelLike(eq(user), any(ProductTarget.class));
            verify(productService).decreaseLikeCount(command.targetId());
        }
    }

    private User createTestUser() {
        UserCommand.Create command = new UserCommand.Create(
                "testUser",
                "test@example.com",
                "1996-08-16",
                Gender.MALE
        );

        return User.of(command);
    }
}
