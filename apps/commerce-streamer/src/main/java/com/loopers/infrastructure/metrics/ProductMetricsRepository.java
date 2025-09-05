package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ProductMetricsRepository extends JpaRepository<ProductMetrics, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query( "SELECT pm " +
            "FROM ProductMetrics pm " +
            "WHERE pm.productId = :productId " +
            "  AND pm.metricDate = :date")
    Optional<ProductMetrics> findByProductIdAndMetricDateWithLock(
            @Param("productId") Long productId, 
            @Param("date") LocalDate date
    );
    
    Optional<ProductMetrics> findByProductIdAndMetricDate(Long productId, LocalDate date);
}
