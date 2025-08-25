package com.loopers.domain.point;

import java.util.List;
import java.util.Optional;

public interface PointHistoryRepository {

    PointHistory save(PointHistory pointHistory);

    List<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    Optional<PointHistory> findByReferenceTypeAndReferenceIdAndTransactionType(
            ReferenceType referenceType, 
            Long referenceId, 
            PointTransactionType transactionType
    );
}
