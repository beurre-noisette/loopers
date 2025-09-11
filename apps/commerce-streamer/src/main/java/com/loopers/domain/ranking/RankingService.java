package com.loopers.domain.ranking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RankingService {
    
    private static final String KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long TTL_DAYS = 2;
    
    @Qualifier("masterRedisTemplate")
    private final RedisTemplate<String, String> redisTemplate;

    public RankingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void incrementScore(Long productId, double score, LocalDate date) {
        String key = generateKey(date);
        String member = generateMember(productId);
        
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        Double newScore = zSetOps.incrementScore(key, member, score);
        
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
        
        log.debug("랭킹 점수 업데이트 - key: {}, productId: {}, delta: {}, newScore: {}", 
                key, productId, score, newScore);
    }

    private String generateKey(LocalDate date) {
        return KEY_PREFIX + date.format(DATE_FORMATTER);
    }
    
    private String generateMember(Long productId) {
        return "product:" + productId;
    }
}
