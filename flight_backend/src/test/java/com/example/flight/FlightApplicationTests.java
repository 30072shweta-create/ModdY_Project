package com.example.flight;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FlightApplicationTests {

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private org.springframework.cache.CacheManager cacheManager;

    @Test
    void contextLoads() {
    }

}
