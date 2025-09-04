package com.loopers.infrastructure.event;

import com.loopers.domain.event.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    
    boolean existsByEventId(String eventId);
    
    Optional<EventLog> findByEventId(String eventId);
}