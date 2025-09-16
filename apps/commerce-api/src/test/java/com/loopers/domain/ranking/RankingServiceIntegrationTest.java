package com.loopers.domain.ranking;

import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class RankingServiceIntegrationTest {
    
    @Autowired
    private RankingService rankingService;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private RedisCleanUp redisCleanUp;
    
    private final LocalDate testDate = LocalDate.of(2025, 1, 11);
    
    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }
    
    @DisplayName("랭킹 데이터 준비")
    @BeforeEach
    void setUpRankingData() {
        // Redis에 직접 테스트 데이터 설정
        String key = "ranking:all:" + testDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        
        zSetOps.add(key, "product:1", 100.0);
        zSetOps.add(key, "product:2", 80.0);
        zSetOps.add(key, "product:3", 60.0);
        zSetOps.add(key, "product:4", 40.0);
        zSetOps.add(key, "product:5", 20.0);
    }
    
    @DisplayName("랭킹 조회 시")
    @Nested
    class GetRankings {
        
        @DisplayName("페이징된 랭킹 목록이 점수 내림차순으로 반환된다")
        @Test
        void returnPagedRankingsInDescOrder_whenGetRankingsWithPaging() {
            // arrange & act
            Set<ZSetOperations.TypedTuple<String>> rankings = 
                rankingService.getRankingsWithPaging(testDate, 0, 3);
            
            // assert
            assertThat(rankings).hasSize(3);
            
            Double previousScore = null;
            for (ZSetOperations.TypedTuple<String> tuple : rankings) {
                if (previousScore != null) {
                    assertThat(tuple.getScore()).isLessThan(previousScore);
                }
                previousScore = tuple.getScore();
            }
        }
        
        @DisplayName("특정 상품의 순위가 정확히 반환된다")
        @Test
        void returnCorrectRank_whenGetProductRank() {
            // arrange & act
            Long rank1 = rankingService.getProductRank(1L, testDate);
            Long rank3 = rankingService.getProductRank(3L, testDate);
            Long rank5 = rankingService.getProductRank(5L, testDate);
            
            // assert
            assertAll(
                () -> assertThat(rank1).isEqualTo(1L),
                () -> assertThat(rank3).isEqualTo(3L),
                () -> assertThat(rank5).isEqualTo(5L)
            );
        }
        
        @DisplayName("랭킹에 없는 상품 조회 시 null이 반환된다")
        @Test
        void returnNull_whenProductNotInRanking() {
            // arrange & act
            Long rank = rankingService.getProductRank(999L, testDate);

            // assert
            assertThat(rank).isNull();
        }
        
        @DisplayName("전체 랭킹 개수가 정확히 반환된다")
        @Test
        void returnCorrectTotalCount_whenGetTotalCount() {
            // arrange & act
            Long count = rankingService.getTotalCount(testDate);
            
            // assert
            assertThat(count).isEqualTo(5L);
        }
    }
    
    @DisplayName("날짜별 키 관리 시")
    @Nested
    class DateBasedKeyManagement {
        
        @DisplayName("서로 다른 날짜의 랭킹은 독립적으로 조회된다")
        @Test
        void queryRankingsIndependently_whenDifferentDates() {
            // arrange
            LocalDate today = LocalDate.of(2025, 1, 11);
            LocalDate tomorrow = LocalDate.of(2025, 1, 12);
            
            // Redis에 다른 날짜 데이터 설정
            String tomorrowKey = "ranking:all:" + tomorrow.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            zSetOps.add(tomorrowKey, "product:100", 50.0);
            
            // act
            Long todayRank = rankingService.getProductRank(1L, today);
            Long tomorrowRank = rankingService.getProductRank(100L, tomorrow);
            Long todayRankFor100 = rankingService.getProductRank(100L, today);
            
            // assert
            assertAll(
                () -> assertThat(todayRank).isEqualTo(1L),  // 오늘 날짜의 1번 상품은 1위
                () -> assertThat(tomorrowRank).isEqualTo(1L),  // 내일 날짜의 100번 상품은 1위
                () -> assertThat(todayRankFor100).isNull()  // 오늘 날짜에는 100번 상품 없음
            );
        }
    }
}
