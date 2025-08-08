package com.loopers.infrastructure.point;

import com.loopers.domain.point.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistory, Long> {

    List<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
