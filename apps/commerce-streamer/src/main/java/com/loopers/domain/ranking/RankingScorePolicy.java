package com.loopers.domain.ranking;

import org.springframework.stereotype.Component;

@Component
public class RankingScorePolicy {
    
    private static final double VIEW_WEIGHT = 0.1;
    private static final double LIKE_WEIGHT = 0.3;
    private static final double ORDER_WEIGHT = 0.6;
    
    private static final double VIEW_SCORE = 1.0;
    private static final double LIKE_SCORE = 1.0;
    
    public double calculateViewScore() {
        return VIEW_SCORE * VIEW_WEIGHT;
    }
    
    public double calculateLikeScore(int delta) {
        return LIKE_SCORE * LIKE_WEIGHT * delta;
    }
    
    public double calculateOrderScore(int quantity, Long amount) {
        double baseScore = quantity * ORDER_WEIGHT;
        
        if (amount != null && amount > 0) {
            double amountBonus = 1 + Math.log10(amount.doubleValue() / 10000);
            return baseScore * Math.max(amountBonus, 1.0);
        }
        
        return baseScore;
    }
}
