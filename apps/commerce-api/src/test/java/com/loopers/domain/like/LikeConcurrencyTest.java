package com.loopers.domain.like;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql(scripts = {"/brand-test-data.sql", "/product-test-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class LikeConcurrencyTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User testUser;
    private Target testTarget;

    @BeforeEach
    @Transactional
    void setUp() {
        testUser = User.of(new UserCommand.Create(
                "testUser",
                "test@example.com",
                "1996-08-16",
                Gender.MALE
        ));
        testUser = userRepository.save(testUser);

        testTarget = ProductTarget.of(1L);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일한 사용자가 같은 상품에 동시에 여러 번 좋아요를 눌러도 한 번만 저장된다")
    @Test
    void concurrentLikeCreation_sameUserSameProduct_shouldCreateOnlyOne() throws InterruptedException {
        // arrange
        int threadCount = 10;

        CountDownLatch latch = new CountDownLatch(threadCount);

        // act - 동시에 같은 사용자가 같은 상품에 좋아요
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        likeService.createLike(testUser, testTarget);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // assert - 좋아요는 정확히 1개만 저장되어야 함
        long likeCount = likeRepository.countByTarget(testTarget.getType(), testTarget.getId());

        assertThat(likeCount).isEqualTo(1L);
    }

    @DisplayName("서로 다른 사용자가 같은 상품에 동시에 좋아요를 눌러도 각각 저장된다")
    @Test
    void concurrentLikeCreation_differentUsers_shouldCreateForEach() throws InterruptedException {
        // arrange
        int threadCount = 5;
        List<User> users = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            User user = createTestUser("user" + i, "user" + i + "@test.com");
            users.add(user);
        }

        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // act
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                final User user = users.get(i);
                executor.submit(() -> {
                    try {
                        likeService.createLike(user, testTarget);
                        successCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // assert
        long likeCount = likeRepository.countByTarget(testTarget.getType(), testTarget.getId());

        assertThat(likeCount).isEqualTo(threadCount);
        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    @DisplayName("좋아요 생성과 취소가 동시에 일어나도 정합성이 보장된다")
    @Test
    void concurrentLikeCreateAndCancel_shouldMaintainConsistency() throws InterruptedException {
        // arrange
        int createThreadCount = 5;
        int cancelThreadCount = 5;
        int totalThreadCount = createThreadCount + cancelThreadCount;

        CountDownLatch latch = new CountDownLatch(totalThreadCount);
        AtomicInteger createCount = new AtomicInteger(0);
        AtomicInteger cancelCount = new AtomicInteger(0);

        // 먼저 좋아요 하나 생성해놓기
        likeService.createLike(testUser, testTarget);

        // act - 동시에 좋아요 생성과 취소
        try (ExecutorService executor = Executors.newFixedThreadPool(totalThreadCount)) {
            for (int i = 0; i < createThreadCount; i++) {
                executor.submit(() -> {
                    try {
                        likeService.createLike(testUser, testTarget);
                        createCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            for (int i = 0; i < cancelThreadCount; i++) {
                executor.submit(() -> {
                    try {
                        likeService.cancelLike(testUser, testTarget);
                        cancelCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // assert - 좋아요는 0개 또는 1개여야 함 (중간 상태 없음)
        long finalLikeCount = likeRepository.countByTarget(testTarget.getType(), testTarget.getId());

        assertThat(finalLikeCount).isIn(0L, 1L);
    }

    @DisplayName("대량의 사용자가 동시에 좋아요를 눌러도 정확한 카운트가 유지된다")
    @Test
    void massiveConcurrentLikeCreation_shouldMaintainAccurateCount() throws InterruptedException {
        // arrange
        int userCount = 20;
        List<User> users = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            User user = createTestUser("user" + i, "massive" + i + "@test.com");
            users.add(user);
        }

        CountDownLatch latch = new CountDownLatch(userCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // act
        try (ExecutorService executor = Executors.newFixedThreadPool(userCount)) {
            for (int i = 0; i < userCount; i++) {
                final User user = users.get(i);
                executor.submit(() -> {
                    try {
                        likeService.createLike(user, testTarget);
                        successCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // then
        long finalLikeCount = likeRepository.countByTarget(testTarget.getType(), testTarget.getId());

        assertThat(finalLikeCount).isEqualTo(userCount);
        assertThat(successCount.get()).isEqualTo(userCount);
    }

    private User createTestUser(String userId, String email) {
        User user = User.of(new UserCommand.Create(
                userId,
                email,
                "1996-08-16",
                Gender.MALE
        ));
        return userRepository.save(user);
    }
}
