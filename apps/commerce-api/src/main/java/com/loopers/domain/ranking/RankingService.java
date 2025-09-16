package com.loopers.domain.ranking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Slf4j
@Service
public class RankingService {
    
    private static final String KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public RankingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Set<ZSetOperations.TypedTuple<String>> getRankingsWithPaging(LocalDate date, int page, int size) {
        try {
            String key = generateKey(date);
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            
            long start = (long) page * size;
            long end = start + size - 1;
            
            Set<ZSetOperations.TypedTuple<String>> rankings = 
                    zSetOps.reverseRangeWithScores(key, start, end);
            
            log.debug("랭킹 페이지 조회 - key: {}, page: {}, size: {}, results: {}", 
                    key, page, size, rankings != null ? rankings.size() : 0);
            
            return rankings;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 연결 실패로 랭킹 조회 불가 - date: {}, page: {}, size: {}", date, page, size);
            return null;
        }
    }
    
    public Long getProductRank(Long productId, LocalDate date) {
        try {
            String key = generateKey(date);
            String member = generateMember(productId);
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            
            Long rank = zSetOps.reverseRank(key, member);
            
            log.debug("상품 순위 조회 - key: {}, productId: {}, rank: {}", 
                    key, productId, rank != null ? rank + 1 : null);
            
            return rank != null ? rank + 1 : null;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 연결 실패로 상품 순위 조회 불가 - productId: {}", productId);
            return null;
        }
    }

    public Long getTotalCount(LocalDate date) {
        try {
            String key = generateKey(date);
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            
            return zSetOps.zCard(key);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 연결 실패로 전체 카운트 조회 불가 - date: {}", date);
            return 0L;
        }
    }
    
    private String generateKey(LocalDate date) {
        return KEY_PREFIX + date.format(DATE_FORMATTER);
    }
    
    private String generateMember(Long productId) {
        return "product:" + productId;
    }
    
    public static Long extractProductId(String member) {
        if (member != null && member.startsWith("product:")) {
            try {
                return Long.parseLong(member.substring(8));
            } catch (NumberFormatException e) {
                log.error("Invalid product member format: {}", member);
                return null;
            }
        }
        return null;
    }
}
