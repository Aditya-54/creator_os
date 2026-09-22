package com.creatoros.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "creatoros.sync")
public record SyncProperties(
        long intervalMs,
        long staleAfterMinutes,
        Executor executor,
        Retry retry
) {
    public record Executor(int corePoolSize, int maxPoolSize, int queueCapacity) {
    }

    public record Retry(int maxAttempts, long initialBackoffMs, double backoffMultiplier) {
    }
}
