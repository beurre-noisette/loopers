package com.loopers.infrastructure.dataplatform;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataPlatformEvent {

    private final String eventType;
    private final String eventSource;
    private final Instant timeStamp;
    private final String correlationId;

    private final Long orderId;
    private final Long userId;

    private final Map<String, Object> payload;

    public static DataPlatformEvent.DataPlatformEventBuilder orderEvent(String eventType) {
        return DataPlatformEvent.builder()
                .eventType(eventType)
                .eventSource("commerce-api")
                .timeStamp(Instant.now());
    }
}
