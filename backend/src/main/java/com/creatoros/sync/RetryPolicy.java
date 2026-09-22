package com.creatoros.sync;

import java.util.function.IntConsumer;
import java.util.function.Supplier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bounded retry with exponential backoff for transient sync failures (rate
 * limits, timeouts, transient 5xx). Runs on the sync executor's own worker
 * thread, so a blocking {@link Thread#sleep} between attempts does not tie up
 * an HTTP request thread. Permanent failures still exhaust their attempts
 * (kept simple deliberately - see docs/concurrency.md) but never retry forever.
 */
@Component
@EnableConfigurationProperties(SyncProperties.class)
public class RetryPolicy {

    private final SyncProperties properties;

    public RetryPolicy(SyncProperties properties) {
        this.properties = properties;
    }

    public <T> T executeWithRetry(Supplier<T> action, IntConsumer onRetry) {
        int maxAttempts = Math.max(1, properties.retry().maxAttempts());
        long backoffMs = properties.retry().initialBackoffMs();
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                lastFailure = e;
                if (attempt >= maxAttempts) {
                    break;
                }
                onRetry.accept(attempt);
                sleep(backoffMs);
                backoffMs = (long) (backoffMs * properties.retry().backoffMultiplier());
            }
        }
        throw lastFailure;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry wait interrupted", e);
        }
    }
}
