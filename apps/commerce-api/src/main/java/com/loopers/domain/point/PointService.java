package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PointService {

    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Autowired
    public PointService(PointRepository pointRepository, PointHistoryRepository pointHistoryRepository) {
        this.pointRepository = pointRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    @Transactional(readOnly = true)
    public Point getPoint(Long userId) {
        return pointRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다."));
    }

    @Transactional
    public Point getPointWithLock(Long userId) {
        return pointRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다."));
    }

    @Transactional
    public Point createPointWithInitialAmount(Long userId, BigDecimal initialAmount, PointReference reference) {
        if (pointRepository.findByUserId(userId).isPresent()) {
            throw new CoreException(ErrorType.CONFLICT, "이미 포인트가 존재합니다.");
        }

        Point point = Point.create(userId);

        if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
            point.charge(initialAmount);

            PointHistory pointHistory = PointHistory.create(
                    new PointHistoryCommand.Create(
                            userId,
                            initialAmount,
                            point.getBalance(),
                            PointTransactionType.CHARGE,
                            reference
                    )
            );
            pointHistoryRepository.save(pointHistory);
        }

        return pointRepository.save(point);
    }

    @Transactional
    public void chargePoint(Long userId, BigDecimal amount, PointReference reference) {
        Point point = getPoint(userId);
        point.charge(amount);

        Point savedPoint = pointRepository.save(point);

        PointHistory pointHistory = PointHistory.create(
                new PointHistoryCommand.Create(
                        userId,
                        amount,
                        savedPoint.getBalance(),
                        PointTransactionType.CHARGE,
                        reference
                )
        );
        pointHistoryRepository.save(pointHistory);
    }

    @Transactional
    public void usePoint(Long userId, BigDecimal amount, PointReference reference) {
        Point point = getPointWithLock(userId);
        point.use(amount);

        Point savedPoint = pointRepository.save(point);

        PointHistory pointHistory = PointHistory.create(
                new PointHistoryCommand.Create(
                        userId,
                        amount.negate(),
                        savedPoint.getBalance(),
                        PointTransactionType.USE,
                        reference
                )
        );
        pointHistoryRepository.save(pointHistory);
    }

    @Transactional
    public void usePointForDiscount(Long userId, BigDecimal amount, Long orderId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        Point point = getPointWithLock(userId);
        point.use(amount);

        Point savedPoint = pointRepository.save(point);

        PointHistory pointHistory = PointHistory.create(
                new PointHistoryCommand.Create(
                        userId,
                        amount.negate(),
                        savedPoint.getBalance(),
                        PointTransactionType.ORDER_DISCOUNT,
                        PointReference.order(orderId)
                )
        );
        pointHistoryRepository.save(pointHistory);
    }

}
