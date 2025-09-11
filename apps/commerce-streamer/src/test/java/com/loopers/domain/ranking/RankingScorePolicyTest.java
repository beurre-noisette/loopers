package com.loopers.domain.ranking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;

class RankingScorePolicyTest {
    
    private final RankingScorePolicy rankingScorePolicy = new RankingScorePolicy();
    
    @DisplayName("조회 점수 계산 시")
    @Nested
    class CalculateViewScore {
        
        @DisplayName("조회 이벤트가 발생하면 가중치 0.1이 적용된 점수가 반환된다")
        @Test
        void returnWeightedScore_whenViewEventOccurs() {
            // arrange & act
            double score = rankingScorePolicy.calculateViewScore();
            
            // assert
            assertThat(score).isEqualTo(0.1);
        }
    }
    
    @DisplayName("좋아요 점수 계산 시")
    @Nested
    class CalculateLikeScore {
        
        @DisplayName("좋아요가 증가하면 가중치 0.3이 적용된 양수 점수가 반환된다")
        @Test
        void returnPositiveScore_whenLikeIncreases() {
            // arrange
            int delta = 2;
            
            // act
            double score = rankingScorePolicy.calculateLikeScore(delta);
            
            // assert
            assertThat(score).isEqualTo(0.6); // 2 * 0.3
        }
        
        @DisplayName("좋아요가 취소되면 가중치 0.3이 적용된 음수 점수가 반환된다")
        @Test
        void returnNegativeScore_whenLikeCancelled() {
            // arrange
            int delta = -1;
            
            // act
            double score = rankingScorePolicy.calculateLikeScore(delta);
            
            // assert
            assertThat(score).isEqualTo(-0.3); // -1 * 0.3
        }
    }
    
    @DisplayName("주문 점수 계산 시")
    @Nested
    class CalculateOrderScore {
        
        @DisplayName("일반 주문이 발생하면 수량과 금액에 따른 가중치가 적용된다")
        @Test
        void applyQuantityAndAmountWeight_whenOrderOccurs() {
            // arrange
            int quantity = 2;
            Long amount = 50000L;
            
            // act
            double score = rankingScorePolicy.calculateOrderScore(quantity, amount);
            
            // assert
            assertAll(
                () -> assertThat(score).isGreaterThan(2.0),
                () -> assertThat(score).isLessThan(2.1)
            );
        }
        
        @DisplayName("고액 주문이 발생하면 추가 보너스가 적용된다")
        @Test
        void applyBonusScore_whenHighAmountOrder() {
            // arrange
            int quantity = 1;
            Long amount = 1000000L; // 100만원
            
            // act
            double score = rankingScorePolicy.calculateOrderScore(quantity, amount);
            
            // assert
            assertThat(score).isCloseTo(1.8, within(0.0000001)); // 0.6 * 3
        }
        
        @DisplayName("금액 정보가 없으면 수량에 대한 기본 가중치만 적용된다")
        @Test
        void applyOnlyQuantityWeight_whenAmountIsNull() {
            // arrange
            int quantity = 3;
            Long amount = null;
            
            // act
            double score = rankingScorePolicy.calculateOrderScore(quantity, amount);
            
            // assert
            assertThat(score).isCloseTo(1.8, within(0.0000001)); // 3 * 0.6
        }
    }
}