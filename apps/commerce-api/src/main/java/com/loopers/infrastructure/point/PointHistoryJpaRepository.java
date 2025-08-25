package com.loopers.infrastructure.point;

import com.loopers.domain.point.PointHistory;
import com.loopers.domain.point.PointTransactionType;
import com.loopers.domain.point.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistory, Long> {

    List<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query( "SELECT ph " +
            "FROM PointHistory ph " +
            "WHERE ph.reference.type = :referenceType " +
            "  AND ph.reference.referenceId = :referenceId " +
            "  AND ph.type = :transactionType")
    Optional<PointHistory> findByReferenceTypeAndReferenceIdAndTransactionType(
            @Param("referenceType") ReferenceType referenceType, 
            @Param("referenceId") Long referenceId, 
            @Param("transactionType") PointTransactionType transactionType
    );
}
