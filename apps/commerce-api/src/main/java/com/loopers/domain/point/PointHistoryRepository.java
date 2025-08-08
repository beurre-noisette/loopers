package com.loopers.domain.point;

import java.util.List;

public interface PointHistoryRepository {

    PointHistory save(PointHistory pointHistory);

    List<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
