package com.loopers.application.ranking;

import com.loopers.application.product.ProductQueryRepository;
import com.loopers.domain.ranking.RankingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RankingQuery {
    
    private final RankingService rankingService;
    private final ProductQueryRepository productQueryRepository;

    @Autowired
    public RankingQuery(RankingService rankingService, ProductQueryRepository productQueryRepository) {
        this.rankingService = rankingService;
        this.productQueryRepository = productQueryRepository;
    }

    public RankingPageResult getRankings(String dateStr, int page, int size) {
        LocalDate date = parseDate(dateStr);
        
        // Redis에서 순서가 보장된 랭킹 데이터 조회
        Set<ZSetOperations.TypedTuple<String>> sortedRankingsFromRedis = 
                rankingService.getRankingsWithPaging(date, page, size);
        
        if (sortedRankingsFromRedis == null || sortedRankingsFromRedis.isEmpty()) {
            log.info("랭킹 데이터가 없습니다 - date: {}, page: {}, size: {}", date, page, size);
            return new RankingPageResult(
                    Collections.emptyList(),
                    0L,
                    0,
                    page,
                    size,
                    date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            );
        }
        
        // DB 조회를 위한 productId 리스트 추출
        List<Long> productIdsForDbQuery = sortedRankingsFromRedis.stream()
                .map(tuple -> RankingService.extractProductId(tuple.getValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        // DB에서 상품 정보 일괄 조회 후 빠른 검색을 위해 Map으로 변환
        Map<Long, ProductQueryRepository.ProductQueryData> productInfoById = 
                productQueryRepository.findProductsByIds(productIdsForDbQuery)
                        .stream()
                        .collect(Collectors.toMap(
                                ProductQueryRepository.ProductQueryData::id,
                                product -> product
                        ));
        
        // 페이징에 따른 시작 순위 계산
        long startRank = (long) page * size + 1;

        // Redis 순서를 유지하며 최종 랭킹 데이터 조합
        List<RankingItem> rankedItemsWithProductInfo = new ArrayList<>();
        long currentRank = startRank;

        for (ZSetOperations.TypedTuple<String> redisRankingEntry : sortedRankingsFromRedis) {
            Long productId = RankingService.extractProductId(redisRankingEntry.getValue());
            if (productId != null) {
                ProductQueryRepository.ProductQueryData productInfo = productInfoById.get(productId);
                if (productInfo != null) {
                    rankedItemsWithProductInfo.add(new RankingItem(
                            currentRank++,
                            productId,
                            productInfo.name(),
                            productInfo.description(),
                            productInfo.price(),
                            productInfo.brandId(),
                            productInfo.brandName(),
                            productInfo.likeCount(),
                            roundScore(redisRankingEntry.getScore())
                    ));
                }
            }
        }
        
        Long totalCount = rankingService.getTotalCount(date);
        int totalPages = (int) Math.ceil((double) totalCount / size);
        
        return new RankingPageResult(
                rankedItemsWithProductInfo,
                totalCount,
                totalPages,
                page,
                size,
                date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDate.now();
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            log.warn("날짜 파싱 실패, 오늘 날짜 사용 - input: {}", dateStr);
            return LocalDate.now();
        }
    }
    
    private Double roundScore(Double score) {
        if (score == null) {
            return null;
        }
        return BigDecimal.valueOf(score)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
    
    public record RankingPageResult(
            List<RankingItem> rankings,
            long totalElements,
            int totalPages,
            int currentPage,
            int pageSize,
            String date
    ) {}
    
    public record RankingItem(
            long rank,
            Long productId,
            String productName,
            String productDescription,
            BigDecimal price,
            Long brandId,
            String brandName,
            Integer likeCount,
            Double score
    ) {}
}
