package com.loopers.support.cache;

import java.time.Duration;

public final class CacheConstants {
    
    private CacheConstants() {}
    
    public static final class Keys {
        public static final String PRODUCT_DETAIL_DTO = "product:detail:dto:";
    }
    
    public static final class TTL {
        public static final Duration PRODUCT_DETAIL = Duration.ofMinutes(10);
    }
}
