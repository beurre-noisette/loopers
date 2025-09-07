package com.loopers.infrastructure.kafka;

import com.loopers.config.kafka.KafkaConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(KafkaConfig.class)
public class KafkaConfiguration {
}