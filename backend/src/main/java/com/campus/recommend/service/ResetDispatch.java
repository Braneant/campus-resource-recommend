package com.campus.recommend.service;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Bounded process-local delivery queue; no credentials or addresses are logged. */
@Service
public class ResetDispatch implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ResetDispatch.class);
    private final PasswordResetService service;
    private final ThreadPoolExecutor executor;

    @Autowired
    public ResetDispatch(PasswordResetService service) { this(service, 100); }

    public ResetDispatch(PasswordResetService service, int capacity) {
        this.service = service;
        this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(capacity), job -> {
                    Thread thread = new Thread(job, "password-reset-delivery");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    public void submit(String studentNo, String email) {
        try {
            executor.execute(() -> {
                try { service.request(studentNo, email); }
                catch (RuntimeException ignored) { LOG.warn("Password reset request could not be completed"); }
            });
        } catch (RejectedExecutionException full) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "服务繁忙，请稍后重试");
        }
    }

    /** Diagnostic barrier used by isolated tests; not exposed by any web endpoint. */
    public boolean awaitIdle(Duration timeout) throws InterruptedException {
        try {
            var barrier = new java.util.concurrent.FutureTask<Void>(() -> null);
            if (!executor.getQueue().offer(barrier, timeout.toMillis(), TimeUnit.MILLISECONDS)) return false;
            barrier.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (TimeoutException | java.util.concurrent.ExecutionException failure) {
            return false;
        }
    }

    @Override @PreDestroy public void close() { executor.shutdownNow(); }
}
