package com.loopers.infrastructure.payment.pg.config;

import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class PgClientConfig {

    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(
                3,
                TimeUnit.SECONDS,
                6,
                TimeUnit.SECONDS,
                true
        );
    }

    @Bean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }
}
