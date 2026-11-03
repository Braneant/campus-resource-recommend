package com.campus.recommend.security;

import com.campus.recommend.service.ResetRequestLimiter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResetRequestLimiterTest {
    @Test void limitsRepeatedRequestsFromSameClient() {
        ResetRequestLimiter limiter = new ResetRequestLimiter();
        for (int i = 0; i < 5; i++) assertTrue(limiter.allow("127.0.0.1"));
        assertFalse(limiter.allow("127.0.0.1"));
        assertTrue(limiter.allow("127.0.0.2"));
    }
}
