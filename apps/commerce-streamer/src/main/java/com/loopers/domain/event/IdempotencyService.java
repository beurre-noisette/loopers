package com.loopers.domain.event;

import com.loopers.infrastructure.event.EventHandledRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class IdempotencyService {
    
    private final EventHandledRepository eventHandledRepository;
    
    public IdempotencyService(EventHandledRepository eventHandledRepository) {
        this.eventHandledRepository = eventHandledRepository;
    }
    
    @Transactional
    public boolean isAlreadyProcessed(String eventId, String consumerGroup) {
        boolean exists = eventHandledRepository.existsByEventIdAndConsumerGroup(eventId, consumerGroup);
        
        if (exists) {
            log.debug("이벤트가 이미 처리됨 - eventId: {}, consumerGroup: {}", eventId, consumerGroup);
        }
        
        return exists;
    }
    
    @Transactional
    public void markAsProcessed(String eventId, String consumerGroup) {
        if (!isAlreadyProcessed(eventId, consumerGroup)) {
            EventHandled handled = EventHandled.of(eventId, consumerGroup);
            eventHandledRepository.save(handled);
            log.debug("이벤트 처리 완료 기록 - eventId: {}, consumerGroup: {}", eventId, consumerGroup);
        }
    }
}