package com.loopers.infrastructure.event;

import com.loopers.domain.event.EventHandled;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventHandledRepository extends JpaRepository<EventHandled, Long> {
    
    boolean existsByEventIdAndConsumerGroup(String eventId, String consumerGroup);
}