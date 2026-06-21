package com.zhiling.system.application.service.support;

import com.zhiling.common.exception.ProjectException;
import com.zhiling.model.dto.TaskTemplateScheduleConfigDto;
import com.zhiling.model.enums.TaskTemplateScheduleType;
import com.zhiling.model.enums.TaskTemplateTimeMode;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 护理任务模板结构化调度支持工具。
 *
 * @author zhanghongyu
 */
public final class TaskTemplateScheduleSupport {

    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<Integer> ALLOWED_INTERVAL_MINUTES = Set.of(30, 60, 120);
    private static final int MAX_LOOKAHEAD_DAYS = 366 * 5;

    /**
     * 构造器：TaskTemplateScheduleSupport
     *
     * @author zhanghongyu
     */
    private TaskTemplateScheduleSupport() {
    }

    /**
     * 方法：normalizeTimezone
     *
     * @author zhanghongyu
     */
    public static String normalizeTimezone(String timezone) {
        String normalized = StringUtils.hasText(timezone) ? timezone.trim() : DEFAULT_TIMEZONE;
        try {
            ZoneId.of(normalized);
        } catch (Exception e) {
            throw new ProjectException(400, "时区配置非法：" + normalized);
        }
        return normalized;
    }

    /**
     * 方法：normalizeAndValidate
     *
     * @author zhanghongyu
     */
    public static TaskTemplateScheduleConfigDto normalizeAndValidate(TaskTemplateScheduleConfigDto rawConfig) {
        if (rawConfig == null) {
            throw new ProjectException(400, "调度规则不能为空");
        }
        if (rawConfig.getScheduleType() == null) {
            throw new ProjectException(400, "调度类型不能为空");
        }
        if (rawConfig.getTimeMode() == null) {
            throw new ProjectException(400, "时间模式不能为空");
        }

        TaskTemplateScheduleConfigDto config = TaskTemplateScheduleConfigDto.builder()
                .scheduleType(rawConfig.getScheduleType())
                .timeMode(rawConfig.getTimeMode())
                .weekdays(normalizeIntegerList(rawConfig.getWeekdays()))
                .monthDays(normalizeIntegerList(rawConfig.getMonthDays()))
                .timePoints(normalizeTimePoints(rawConfig.getTimePoints()))
                .startTime(normalizeTimeString(rawConfig.getStartTime()))
                .endTime(normalizeTimeString(rawConfig.getEndTime()))
                .intervalMinutes(rawConfig.getIntervalMinutes())
                .build();

        validateScheduleScope(config);
        validateTimeMode(config);
        return config;
    }

    /**
     * 方法：validateDateRange
     *
     * @author zhanghongyu
     */
    public static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ProjectException(400, "开始日期不能晚于结束日期");
        }
    }

    public static LocalDateTime calculateNextTrigger(TaskTemplateScheduleConfigDto config,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     String timezone,
                                                     LocalDateTime referenceTime) {
        normalizeTimezone(timezone);
        LocalDateTime reference = referenceTime == null ? LocalDateTime.now() : referenceTime;
        LocalDate cursor = startDate != null && startDate.isAfter(reference.toLocalDate())
                ? startDate
                : reference.toLocalDate();

        for (int i = 0; i < MAX_LOOKAHEAD_DAYS; i++) {
            LocalDate candidateDate = cursor.plusDays(i);
            if (endDate != null && candidateDate.isAfter(endDate)) {
                return null;
            }
            if (!matchesScheduleDate(config, candidateDate)) {
                continue;
            }
            for (LocalTime candidateTime : resolveTimeCandidates(config)) {
                LocalDateTime candidate = LocalDateTime.of(candidateDate, candidateTime);
                if (candidate.isAfter(reference)) {
                    return candidate.withSecond(0).withNano(0);
                }
            }
        }
        return null;
    }

    /**
     * 方法：buildScheduleDescription
     *
     * @author zhanghongyu
     */
    public static String buildScheduleDescription(TaskTemplateScheduleConfigDto config) {
        String scope = switch (config.getScheduleType()) {
            case DAILY -> "每天";
            case WEEKLY -> "每周 " + config.getWeekdays().stream()
                    .map(TaskTemplateScheduleSupport::weekdayLabel)
                    .collect(Collectors.joining("、"));
            case MONTHLY -> "每月 " + config.getMonthDays().stream()
                    .map(day -> day + "号")
                    .collect(Collectors.joining("、"));
        };

        String mode = switch (config.getTimeMode()) {
            case POINT -> "定点 " + String.join("、", config.getTimePoints());
            case INTERVAL -> "每" + config.getIntervalMinutes() + "分钟，" + config.getStartTime() + " - " + config.getEndTime();
        };
        return scope + " / " + mode;
    }

    /**
     * 方法：validateScheduleScope
     *
     * @author zhanghongyu
     */
    private static void validateScheduleScope(TaskTemplateScheduleConfigDto config) {
        if (config.getScheduleType() == TaskTemplateScheduleType.WEEKLY) {
            if (config.getWeekdays().isEmpty()) {
                throw new ProjectException(400, "每周调度必须选择周几");
            }
            boolean hasInvalidValue = config.getWeekdays().stream().anyMatch(day -> day < 1 || day > 7);
            if (hasInvalidValue) {
                throw new ProjectException(400, "周几范围必须在 1 到 7 之间");
            }
        }
        if (config.getScheduleType() == TaskTemplateScheduleType.MONTHLY) {
            if (config.getMonthDays().isEmpty()) {
                throw new ProjectException(400, "每月调度必须选择日期");
            }
            boolean hasInvalidValue = config.getMonthDays().stream().anyMatch(day -> day < 1 || day > 31);
            if (hasInvalidValue) {
                throw new ProjectException(400, "每月日期范围必须在 1 到 31 之间");
            }
        }
    }

    /**
     * 方法：validateTimeMode
     *
     * @author zhanghongyu
     */
    private static void validateTimeMode(TaskTemplateScheduleConfigDto config) {
        if (config.getTimeMode() == TaskTemplateTimeMode.POINT) {
            if (config.getTimePoints().isEmpty()) {
                throw new ProjectException(400, "定点模式至少需要一个执行时间");
            }
            return;
        }

        if (!StringUtils.hasText(config.getStartTime()) || !StringUtils.hasText(config.getEndTime())) {
            throw new ProjectException(400, "区间模式必须配置开始时间和结束时间");
        }
        if (config.getIntervalMinutes() == null || !ALLOWED_INTERVAL_MINUTES.contains(config.getIntervalMinutes())) {
            throw new ProjectException(400, "区间步长仅支持 30 / 60 / 120 分钟");
        }

        LocalTime start = parseTime(config.getStartTime(), "开始时间格式错误");
        LocalTime end = parseTime(config.getEndTime(), "结束时间格式错误");
        if (start.isAfter(end)) {
            throw new ProjectException(400, "开始时间不能晚于结束时间");
        }
    }

    /**
     * 方法：matchesScheduleDate
     *
     * @author zhanghongyu
     */
    private static boolean matchesScheduleDate(TaskTemplateScheduleConfigDto config, LocalDate candidateDate) {
        return switch (config.getScheduleType()) {
            case DAILY -> true;
            case WEEKLY -> config.getWeekdays().contains(candidateDate.getDayOfWeek().getValue());
            case MONTHLY -> config.getMonthDays().contains(candidateDate.getDayOfMonth());
        };
    }

    /**
     * 方法：resolveTimeCandidates
     *
     * @author zhanghongyu
     */
    private static List<LocalTime> resolveTimeCandidates(TaskTemplateScheduleConfigDto config) {
        if (config.getTimeMode() == TaskTemplateTimeMode.POINT) {
            return config.getTimePoints().stream()
                    .map(time -> parseTime(time, "定点时间格式错误"))
                    .sorted()
                    .toList();
        }

        LocalTime start = parseTime(config.getStartTime(), "开始时间格式错误");
        LocalTime end = parseTime(config.getEndTime(), "结束时间格式错误");
        List<LocalTime> candidates = new ArrayList<>();
        for (LocalTime current = start; !current.isAfter(end); current = current.plusMinutes(config.getIntervalMinutes())) {
            candidates.add(current);
        }
        return candidates;
    }

    /**
     * 方法：normalizeIntegerList
     *
     * @author zhanghongyu
     */
    private static List<Integer> normalizeIntegerList(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .sorted()
                .toList();
    }

    /**
     * 方法：normalizeTimePoints
     *
     * @author zhanghongyu
     */
    private static List<String> normalizeTimePoints(List<String> timePoints) {
        if (timePoints == null || timePoints.isEmpty()) {
            return List.of();
        }
        return timePoints.stream()
                .filter(StringUtils::hasText)
                .map(TaskTemplateScheduleSupport::normalizeTimeString)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    /**
     * 方法：normalizeTimeString
     *
     * @author zhanghongyu
     */
    private static String normalizeTimeString(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseTime(value.trim(), "时间格式错误").format(TIME_FORMATTER);
    }

    /**
     * 方法：parseTime
     *
     * @author zhanghongyu
     */
    private static LocalTime parseTime(String value, String errorMessage) {
        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ProjectException(400, errorMessage + "：" + value);
        }
    }

    /**
     * 方法：weekdayLabel
     *
     * @author zhanghongyu
     */
    private static String weekdayLabel(int weekday) {
        DayOfWeek dayOfWeek = DayOfWeek.of(weekday);
        return switch (dayOfWeek) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }
}