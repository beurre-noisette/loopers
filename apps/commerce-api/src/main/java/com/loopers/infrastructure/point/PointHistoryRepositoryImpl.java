package com.loopers.infrastructure.point;

import com.loopers.domain.point.PointHistory;
import com.loopers.domain.point.PointHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.loopers.domain.point.PointTransactionType;
import com.loopers.domain.point.ReferenceType;

import java.util.List;
import java.util.Optional;

@Repository
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

    private final PointHistoryJpaRepository pointHistoryJpaRepository;

    @Autowired
    public PointHistoryRepositoryImpl(PointHistoryJpaRepository pointHistoryJpaRepository) {
        this.pointHistoryJpaRepository = pointHistoryJpaRepository;
    }

    @Override
    public PointHistory save(PointHistory pointHistory) {
        return pointHistoryJpaRepository.save(pointHistory);
    }

    @Override
    public List<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Optional<PointHistory> findByReferenceTypeAndReferenceIdAndTransactionType(
            ReferenceType referenceType, 
            Long referenceId, 
            PointTransactionType transactionType
    ) {
        return pointHistoryJpaRepository.findByReferenceTypeAndReferenceIdAndTransactionType(
                referenceType, referenceId, transactionType
        );
    }
}
