package com.campus.recommend.security;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.service.PasswordResetService;
import com.campus.recommend.service.ResetDispatch;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ResetDispatchTest {
    @Test void queuesBothAccountsAndDoesNotExposeDeliveryFailure() throws Exception {
        PasswordResetService service = mock(PasswordResetService.class);
        doThrow(new IllegalStateException("sensitive")).when(service).request("unknown", "none");
        try (ResetDispatch queue = new ResetDispatch(service, 3)) {
            queue.submit("known", "mail"); queue.submit("unknown", "none");
            assertTrue(queue.awaitIdle(Duration.ofSeconds(3)));
            verify(service).request("known", "mail"); verify(service).request("unknown", "none");
        }
    }

    @Test void boundedQueueRejectsWithoutCheckingAccount() throws Exception {
        PasswordResetService service = mock(PasswordResetService.class);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        doAnswer(invocation -> { entered.countDown(); release.await(3, TimeUnit.SECONDS); return null; })
                .when(service).request(anyString(), anyString());
        try (ResetDispatch queue = new ResetDispatch(service, 1)) {
            queue.submit("first", "mail"); assertTrue(entered.await(2, TimeUnit.SECONDS));
            queue.submit("second", "mail");
            assertThrows(BusinessException.class, () -> queue.submit("third", "mail"));
            release.countDown(); assertTrue(queue.awaitIdle(Duration.ofSeconds(3)));
        } finally { release.countDown(); }
    }
}
