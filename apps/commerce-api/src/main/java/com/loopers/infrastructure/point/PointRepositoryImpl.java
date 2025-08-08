package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PointRepositoryImpl implements PointRepository {

    private final PointJpaRepository pointJpaRepository;

    @Autowired
    public PointRepositoryImpl(PointJpaRepository pointJpaRepository) {
        this.pointJpaRepository = pointJpaRepository;
    }


    @Override
    public Point save(Point point) {
        return pointJpaRepository.save(point);
    }

    @Override
    public Optional<Point> findByUserId(Long userId) {
        return pointJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<Point> findByUserIdWithLock(Long userId) {
        return pointJpaRepository.findByUserIdWithLock(userId);
    }
}
