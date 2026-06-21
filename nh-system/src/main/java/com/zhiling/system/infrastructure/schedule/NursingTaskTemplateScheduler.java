package com.zhiling.system.infrastructure.schedule;

import com.zhiling.system.application.service.NursingTaskTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 护理任务模板定时生成调度器。
 *
 * 每分钟扫描所有到期模板，根据结构化调度规则推进任务生成。
 *
 * @author zhanghongyu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NursingTaskTemplateScheduler {

    private static final long FIXED_DELAY_MS = 60 * 1000L;
    private static final long INITIAL_DELAY_MS = 60 * 1000L;

    private final NursingTaskTemplateService templateService;

    /**
     * 方法：logSchedulerStarted
     *
     * @author zhanghongyu
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logSchedulerStarted() {
        log.info("[TaskTemplate] 护理任务模板定时任务启动成功: fixedDelayMs={}, initialDelayMs={}, scanRule=next_trigger_time <= now",
                FIXED_DELAY_MS,
                INITIAL_DELAY_MS);
    }

    /**
     * 方法：generateScheduledTasks
     *
     * @author zhanghongyu
     */
    @Scheduled(fixedDelay = FIXED_DELAY_MS, initialDelay = INITIAL_DELAY_MS)
    public void generateScheduledTasks() {
        try {
            int count = templateService.generateAllDueTasks();
            if (count > 0) {
                log.info("[TaskTemplate] 自动生成 {} 条护理任务", count);
            }
        } catch (Exception e) {
            log.error("[TaskTemplate] 定时生成任务失败: {}", e.getMessage(), e);
        }
    }
}