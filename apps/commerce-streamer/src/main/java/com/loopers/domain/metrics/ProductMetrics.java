package com.loopers.domain.metrics;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "product_metrics", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"productId", "metricDate"})
       },
       indexes = {
           @Index(name = "idx_product_metrics_date", columnList = "metricDate"),
           @Index(name = "idx_product_metrics_product", columnList = "productId")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetrics extends BaseEntity {
    
    @Column(nullable = false)
    private Long productId;
    
    @Column(nullable = false)
    private LocalDate metricDate;
    
    @Column(nullable = false)
    private Long likeCount = 0L;
    
    @Column(nullable = false)
    private Long salesCount = 0L;
    
    @Column(nullable = false)
    private Long viewCount = 0L;
    
    @Column(nullable = false)
    private Long totalSalesAmount = 0L;
    
    @Version
    private Long version;
    
    @Column(nullable = false)
    private ZonedDateTime lastUpdatedAt;
    
    @Builder
    public ProductMetrics(Long productId, LocalDate metricDate) {
        this.productId = productId;
        this.metricDate = metricDate;
        this.likeCount = 0L;
        this.salesCount = 0L;
        this.viewCount = 0L;
        this.totalSalesAmount = 0L;
        this.lastUpdatedAt = ZonedDateTime.now();
    }
    
    public void incrementLikeCount(int delta) {
        this.likeCount = Math.max(0, this.likeCount + delta);
        this.lastUpdatedAt = ZonedDateTime.now();
    }
    
    public void incrementSalesCount(int quantity, Long amount) {
        this.salesCount += quantity;
        this.totalSalesAmount += amount;
        this.lastUpdatedAt = ZonedDateTime.now();
    }
    
    public void incrementViewCount() {
        this.viewCount++;
        this.lastUpdatedAt = ZonedDateTime.now();
    }
    
    public static ProductMetrics createForToday(Long productId) {
        return ProductMetrics.builder()
                .productId(productId)
                .metricDate(LocalDate.now())
                .build();
    }
}