package com.zhiling.framework.autoconfigure.concurrent;

import com.zhiling.common.utils.Threads;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 平台级线程池配置。
 *
 * @author zhanghongyu
 */
@AutoConfiguration
public class ThreadPoolAutoConfiguration {

    private final int corePoolSize = 50;
    private final int maxPoolSize = 200;
    private final int queueCapacity = 1000;
    private final int keepAliveSeconds = 300;

    /**
     * 方法：threadPoolTaskExecutor
     *
     * @author zhanghongyu
     */
    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(maxPoolSize);
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * 方法：scheduledExecutorService
     *
     * @author zhanghongyu
     */
    @Bean(name = "scheduledExecutorService")
    protected ScheduledExecutorService scheduledExecutorService() {
        return new ScheduledThreadPoolExecutor(corePoolSize,
                new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d")
                        .daemon(true).build(),
                new ThreadPoolExecutor.CallerRunsPolicy()) {
            /**
             * 方法：afterExecute
             *
             * @author zhanghongyu
             */
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                Threads.printException(r, t);
            }
        };
    }
}