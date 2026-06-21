package com.zhiling.system.infrastructure.schedule;

import com.zhiling.system.application.service.NursingTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 护理任务状态扫描调度器。
 *
 * 每分钟扫描所有任务状态：
 * 1) 已到计划开始时间的待执行任务自动标记为执行中；
 * 2) 已超过计划结束时间且未完成任务自动标记为超时。
 *
 * @author zhanghongyu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NursingTaskOverdueScheduler {

    private static final long FIXED_DELAY_MS = 60 * 1000L;
    private static final long INITIAL_DELAY_MS = 60 * 1000L;

    private final NursingTaskService nursingTaskService;

    /**
     * 方法：logSchedulerStarted
     *
     * @author zhanghongyu
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logSchedulerStarted() {
        log.info("[TaskStatusSync] 护理任务状态扫描定时任务启动成功: fixedDelayMs={}, initialDelayMs={}, scanRule=pending->running && (pending|running)->overdue",
                FIXED_DELAY_MS,
                INITIAL_DELAY_MS);
    }

    /**
     * 方法：syncTaskExecutionStatus
     *
     * @author zhanghongyu
     */
    @Scheduled(fixedDelay = FIXED_DELAY_MS, initialDelay = INITIAL_DELAY_MS)
    public void syncTaskExecutionStatus() {
        try {
            nursingTaskService.syncTaskExecutionStatus();
        } catch (Exception e) {
            log.error("[TaskStatusSync] 定时扫描任务状态失败: {}", e.getMessage(), e);
        }
    }
}