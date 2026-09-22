package com.creatoros.sync;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bounded thread pool for concurrent account synchronization. Sized from
 * configuration (never hardcoded) per docs/concurrency.md. Uses
 * {@link ThreadPoolExecutor.CallerRunsPolicy} as the rejection policy so a
 * saturated queue slows the scheduler down instead of silently dropping sync
 * work.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(SyncProperties.class)
public class SyncExecutorConfig {

    @Bean(name = "syncTaskExecutor")
    public ThreadPoolTaskExecutor syncTaskExecutor(SyncProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.executor().corePoolSize());
        executor.setMaxPoolSize(properties.executor().maxPoolSize());
        executor.setQueueCapacity(properties.executor().queueCapacity());
        executor.setThreadNamePrefix("sync-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
