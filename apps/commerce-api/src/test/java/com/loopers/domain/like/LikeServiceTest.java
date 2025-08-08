package com.loopers.domain.like;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @InjectMocks
    private LikeService likeService;

    @DisplayName("좋아요 등록 시")
    @Nested
    class Create {

        @DisplayName("새로운 좋아요가 성공적으로 등록된다.")
        @Test
        void createLike_success() {
            // arrange
            User user = createTestUser();
            Target target = ProductTarget.of(1L);

            when(likeRepository.existsByUserAndTarget(user, target.getType(), target.getId()))
                    .thenReturn(false);

            // act
            likeService.createLike(user, target);

            // assert
            verify(likeRepository).existsByUserAndTarget(user, target.getType(), target.getId());
            verify(likeRepository).save(any(Like.class));
        }

        @DisplayName("이미 존재하는 좋아요는 중복 등록되지 않는다.")
        @Test
        void createLike_idempotent_whenAlreadyExists() {
            // arrange
            User user = createTestUser();
            Target target = ProductTarget.of(1L);

            when(likeRepository.existsByUserAndTarget(user, target.getType(), target.getId()))
                    .thenReturn(true);

            // act
            likeService.createLike(user, target);

            // assert
            verify(likeRepository).existsByUserAndTarget(user, target.getType(), target.getId());
            verify(likeRepository, never()).save(any(Like.class));
        }
    }

    @DisplayName("좋아요 취소 시")
    @Nested
    class Cancel {

        @DisplayName("존재하는 좋아요가 성공적으로 취소된다.")
        @Test
        void cancelLike_success() {
            // arrange
            User user = createTestUser();
            Target target = ProductTarget.of(1L);

            // act
            likeService.cancelLike(user, target);

            // assert
            verify(likeRepository).deleteByUserAndTarget(user, target.getType(), target.getId());
        }

        @DisplayName("존재하지 않는 좋아요 취소는 아무 동작을 하지 않는다.")
        @Test
        void cancelLike_idempotent_whenNotExists() {
            // arrange
            User user = createTestUser();
            Target target = ProductTarget.of(1L);

            // act
            likeService.cancelLike(user, target);

            // assert
            verify(likeRepository).deleteByUserAndTarget(user, target.getType(), target.getId());
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
