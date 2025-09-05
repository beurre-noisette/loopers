package com.loopers.domain.event;

import com.loopers.infrastructure.event.EventHandledRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
    public boolean tryMarkAsProcessed(String eventId, String consumerGroup) {
        try {
            EventHandled handled = EventHandled.of(eventId, consumerGroup);
            eventHandledRepository.save(handled);
            log.debug("이벤트 처리 완료 기록 - eventId: {}, consumerGroup: {}", eventId, consumerGroup);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Unique 제약 위반 = 이미 다른 스레드가 처리함
            log.debug("이벤트가 이미 처리됨 (동시성) - eventId: {}, consumerGroup: {}", eventId, consumerGroup);
            return false;
        }
    }
}
