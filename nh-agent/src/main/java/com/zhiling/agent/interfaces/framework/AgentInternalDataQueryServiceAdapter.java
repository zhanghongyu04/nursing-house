package com.zhiling.agent.interfaces.framework;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.zhiling.agent.application.AgentInternalDataQueryService;
import com.zhiling.agent.infrastructure.mcp.AgentInternalDataMcp;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 不依赖模型工具调用，直接在后端识别并执行内部数据查询。
 *
 * @author zhanghongyu
 */
@Component
public class AgentInternalDataQueryServiceAdapter implements AgentInternalDataQueryService {

    private static final Pattern DETAIL_NAME_PATTERN = Pattern.compile("(?:请)?(?:查询|查看|统计|获取|分析)?(.+?)(?:的)?(?:机构详情|详情统计|概览统计|机构概况|概况统计|床位使用率|床位统计|入住情况|统一社会信用代码|社会信用代码|信用代码|法人姓名|法人联系方式|法人电话|机构地址|运营状态)");
    private static final Pattern CARE_LEVEL_NAME_PATTERN = Pattern.compile("(?:请)?(?:查询|查看|统计|获取|分析)?(.+?)(?:的)?(?:老人)?(?:自理能力|能力|失能等级)分布");
    private static final Pattern GENERIC_SANATORIUM_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9（）()·\\-]{2,40}(?:社会福利院|福利院|养老院|养老机构|康养中心|护理院|敬老院|老年公寓|颐养中心|养老服务中心))");
    private static final String[] GLOBAL_SCOPE_WORDS = {"全局", "全市", "全部", "平台", "系统", "当前系统"};
    private static final String[] CURRENT_SCOPE_WORDS = {"当前机构", "本机构", "本院", "我院", "所在机构", "当前授权范围", "当前授权机构范围", "授权机构范围", "授权范围"};
    private static final String[] REGION_QUERY_WORDS = {"区县养老机构数量分布", "区域养老机构数量分布", "各区县养老机构数量分布", "区域机构分布", "机构区域分布", "区县分布"};
    private static final String[] REGION_SCOPE_WORDS = {"区县", "区域", "行政区", "街道"};
    private static final String[] DISTRIBUTION_WORDS = {"数量分布", "机构分布", "分布情况", "分布结果"};
    private static final String[] CARE_LEVEL_WORDS = {"自理能力分布", "能力分布", "失能分布", "失能等级分布", "能力完好", "轻度失能", "中度失能", "重度失能", "完全失能"};
    private static final String[] NURSING_TASK_WORDS = {"护理任务", "任务列表", "正常任务", "正常护理任务", "异常任务", "异常护理任务", "待办任务", "执行中任务", "已完成任务", "超时任务", "已取消任务", "逾期任务"};
    private static final String[] NURSING_LOG_WORDS = {"护理日志", "护理记录", "日志列表", "正常日志", "正常护理日志", "普通日志", "普通护理日志", "异常日志", "异常护理", "护理执行记录"};
    private static final String[] DETAIL_QUERY_WORDS = {"机构详情", "详情统计", "机构概况", "概况统计", "机构信息", "机构画像", "床位使用率", "床位统计", "入住情况", "统一社会信用代码", "社会信用代码", "信用代码", "法人姓名", "法人联系方式", "法人电话"};
    private static final String[] DETAIL_METRIC_WORDS = {"老人数量", "护理人员数量", "医护人员数量", "床位总数", "已用床位数", "床位使用率", "入住人数", "统一社会信用代码", "社会信用代码", "信用代码", "法人姓名", "法人联系方式", "法人电话", "机构地址", "运营状态"};
    private static final String[] OVERVIEW_QUERY_WORDS = {"系统概览", "概览统计", "平台概览", "系统统计", "平台统计", "总体情况", "整体情况", "总体统计"};
    private static final String[] OVERVIEW_METRIC_WORDS = {"老人数量", "机构数量", "护理人员数量", "医护人员数量", "床位总数", "已用床位数", "床位使用率"};
    private static final String[] NOISE_PREFIX_WORDS = {"请帮我", "帮我", "请问", "麻烦", "请", "查询", "查看", "统计", "获取", "分析", "一下", "一下子", "当前", "目前", "现在", "关于", "有关"};
    private static final String[] NOISE_SUFFIX_WORDS = {"的数据", "数据", "情况", "信息", "详情", "统计", "概览", "概况", "当前"};

    private final AgentInternalDataMcp agentInternalDataMcp;

    /**
     * 构造器：AgentInternalDataQueryServiceAdapter
     *
     * @author zhanghongyu
     */
    public AgentInternalDataQueryServiceAdapter(AgentInternalDataMcp agentInternalDataMcp) {
        this.agentInternalDataMcp = agentInternalDataMcp;
    }

    /**
     * 方法：tryHandle
     *
     * @author zhanghongyu
     */
    @Override
    public Optional<String> tryHandle(String prompt) {
        if (StrUtil.isBlank(prompt)) {
            return Optional.empty();
        }

        QueryContext context = buildContext(prompt);
        QueryIntent intent = resolveIntent(context);
        if (intent == null) {
            return Optional.empty();
        }

        if (isNursingTaskQuery(context) && isNursingLogQuery(context)) {
            return Optional.of(renderCombinedResults(List.of(
                    renderNursingTasks(agentInternalDataMcp.listNursingTasks(
                            context.sanaName(), resolveTaskStatus(context), resolveNormalTaskOnly(context), resolveAbnormalTaskOnly(context), null)),
                    renderNursingLogs(agentInternalDataMcp.listNursingLogs(
                            context.sanaName(), resolveAbnormalFlag(context), null))
            )));
        }

        if (intent == QueryIntent.REGION_DISTRIBUTION) {
            return Optional.of(renderRegionDistribution(agentInternalDataMcp.getRegionSanatoriumDistribution()));
        }

        if (intent == QueryIntent.CARE_LEVEL_DISTRIBUTION) {
            return Optional.of(renderCareLevelDistribution(agentInternalDataMcp.getElderCareLevelStats(context.sanaName())));
        }

        if (intent == QueryIntent.NURSING_TASKS) {
            return Optional.of(renderNursingTasks(agentInternalDataMcp.listNursingTasks(
                    context.sanaName(), resolveTaskStatus(context), resolveNormalTaskOnly(context), resolveAbnormalTaskOnly(context), null)));
        }

        if (intent == QueryIntent.NURSING_LOGS) {
            return Optional.of(renderNursingLogs(agentInternalDataMcp.listNursingLogs(
                    context.sanaName(), resolveAbnormalFlag(context), null)));
        }

        if (intent == QueryIntent.SANATORIUM_DETAIL) {
            return Optional.of(renderSanatoriumDetail(agentInternalDataMcp.getSanatoriumDetailStats(context.sanaName())));
        }

        if (intent == QueryIntent.OVERVIEW) {
            return Optional.of(renderOverview(agentInternalDataMcp.getSystemOverviewStats()));
        }

        return Optional.empty();
    }

    /**
     * 方法：buildContext
     *
     * @author zhanghongyu
     */
    private QueryContext buildContext(String prompt) {
        String normalizedPrompt = StrUtil.trim(prompt);
        String compactPrompt = normalizePrompt(normalizedPrompt);
        boolean explicitGlobal = StrUtil.containsAny(normalizedPrompt, GLOBAL_SCOPE_WORDS);
        boolean explicitCurrentOrg = StrUtil.containsAny(normalizedPrompt, CURRENT_SCOPE_WORDS);
        String sanaName = extractSanaName(normalizedPrompt);
        return new QueryContext(normalizedPrompt, compactPrompt, sanaName, explicitGlobal, explicitCurrentOrg);
    }

    /**
     * 方法：resolveIntent
     *
     * @author zhanghongyu
     */
    private QueryIntent resolveIntent(QueryContext context) {
        if (isRegionDistributionQuery(context)) {
            return QueryIntent.REGION_DISTRIBUTION;
        }

        if (isCareLevelQuery(context)) {
            return QueryIntent.CARE_LEVEL_DISTRIBUTION;
        }

        if (isNursingTaskQuery(context)) {
            return QueryIntent.NURSING_TASKS;
        }

        if (isNursingLogQuery(context)) {
            return QueryIntent.NURSING_LOGS;
        }

        if (isSanatoriumDetailQuery(context)) {
            return QueryIntent.SANATORIUM_DETAIL;
        }

        if (isOverviewQuery(context)) {
            return QueryIntent.OVERVIEW;
        }

        return null;
    }

    /**
     * 方法：isOverviewQuery
     *
     * @author zhanghongyu
     */
    private boolean isOverviewQuery(QueryContext context) {
        if (context.sanaName() != null) {
            return false;
        }

        return containsAny(context, OVERVIEW_QUERY_WORDS)
                || (hasAny(context, OVERVIEW_METRIC_WORDS) && (context.explicitGlobal() || context.explicitCurrentOrg()))
                || (hasAny(context, "机构数量") && hasAny(context, "老人数量", "床位使用率", "床位总数", "已用床位数"));
    }

    /**
     * 方法：isRegionDistributionQuery
     *
     * @author zhanghongyu
     */
    private boolean isRegionDistributionQuery(QueryContext context) {
        return containsAny(context, REGION_QUERY_WORDS)
                || (hasAny(context, REGION_SCOPE_WORDS) && hasAny(context, DISTRIBUTION_WORDS))
                || (hasAny(context, REGION_SCOPE_WORDS) && hasAny(context, "机构数量", "养老机构数量"));
    }

    /**
     * 方法：isSanatoriumDetailQuery
     *
     * @author zhanghongyu
     */
    private boolean isSanatoriumDetailQuery(QueryContext context) {
        if (containsAny(context, DETAIL_QUERY_WORDS) && (context.sanaName() != null || context.explicitCurrentOrg())) {
            return true;
        }

        if (context.sanaName() != null && hasAny(context, DETAIL_METRIC_WORDS)) {
            return true;
        }

        return context.explicitCurrentOrg() && hasAny(context, DETAIL_METRIC_WORDS);
    }

    /**
     * 方法：isCareLevelQuery
     *
     * @author zhanghongyu
     */
    private boolean isCareLevelQuery(QueryContext context) {
        return containsAny(context, CARE_LEVEL_WORDS);
    }

    private boolean isNursingTaskQuery(QueryContext context) {
        return containsAny(context, NURSING_TASK_WORDS)
                || (hasAny(context, "任务") && hasAny(context, "护理", "护士", "老人"));
    }

    private boolean isNursingLogQuery(QueryContext context) {
        return containsAny(context, NURSING_LOG_WORDS)
                || (hasAny(context, "日志", "记录") && hasAny(context, "护理", "护士", "老人"));
    }

    /**
     * 方法：containsAny
     *
     * @author zhanghongyu
     */
    private boolean containsAny(QueryContext context, String... words) {
        return StrUtil.containsAny(context.originalPrompt(), words) || StrUtil.containsAny(context.compactPrompt(), words);
    }

    /**
     * 方法：hasAny
     *
     * @author zhanghongyu
     */
    private boolean hasAny(QueryContext context, String... words) {
        return containsAny(context, words);
    }

    /**
     * 方法：normalizePrompt
     *
     * @author zhanghongyu
     */
    private String normalizePrompt(String prompt) {
        return prompt.replaceAll("[\\s，。！？、；：,.!?;:\"'“”‘’（）()【】\\[\\]]+", "");
    }

    /**
     * 方法：extractSanaName
     *
     * @author zhanghongyu
     */
    private String extractSanaName(String prompt) {
        if (StrUtil.containsAny(prompt, CURRENT_SCOPE_WORDS) || StrUtil.containsAny(prompt, GLOBAL_SCOPE_WORDS)) {
            return null;
        }

        for (Pattern pattern : List.of(DETAIL_NAME_PATTERN, CARE_LEVEL_NAME_PATTERN, GENERIC_SANATORIUM_PATTERN)) {
            Matcher matcher = pattern.matcher(prompt);
            if (!matcher.find()) {
                continue;
            }
            String candidate = sanitizeSanaName(matcher.group(1));
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 方法：sanitizeSanaName
     *
     * @author zhanghongyu
     */
    private String sanitizeSanaName(String candidate) {
        if (StrUtil.isBlank(candidate)) {
            return null;
        }

        String sanitized = StrUtil.trim(candidate);
        sanitized = StrUtil.subBefore(sanitized, "包括", false);
        sanitized = StrUtil.subBefore(sanitized, "并", false);
        sanitized = StrUtil.subBefore(sanitized, "分别", false);
        sanitized = StrUtil.subBefore(sanitized, "以及", false);
        sanitized = StrUtil.trim(sanitized);

        boolean changed;
        do {
            changed = false;
            for (String prefix : NOISE_PREFIX_WORDS) {
                if (StrUtil.startWith(sanitized, prefix)) {
                    sanitized = StrUtil.trim(StrUtil.removePrefix(sanitized, prefix));
                    changed = true;
                }
            }
        } while (changed);

        for (String suffix : NOISE_SUFFIX_WORDS) {
            if (StrUtil.endWith(sanitized, suffix)) {
                sanitized = StrUtil.trim(StrUtil.removeSuffix(sanitized, suffix));
            }
        }

        sanitized = StrUtil.removeSuffix(sanitized, "的");
        sanitized = StrUtil.removeSuffix(sanitized, "当前");
        sanitized = StrUtil.trim(sanitized);

        if (StrUtil.isBlank(sanitized)
                || StrUtil.equalsAnyIgnoreCase(sanitized, GLOBAL_SCOPE_WORDS)
                || StrUtil.equalsAnyIgnoreCase(sanitized, CURRENT_SCOPE_WORDS)) {
            return null;
        }

        return sanitized;
    }

    /**
     * 方法：renderOverview
     *
     * @author zhanghongyu
     */
    private String renderOverview(Map<String, Object> result) {
        if (!(Boolean.TRUE.equals(result.get("success")))) {
            return renderFailure(result);
        }

        Map<String, Object> data = castMap(result.get("data"));
        StringBuilder answer = new StringBuilder();
        answer.append("## 系统概览统计\n\n");
        answer.append(buildMetaBlock(String.valueOf(data.get("scopeName")), "系统实时统计")).append("\n");
        answer.append("| 指标 | 数值 |\n");
        answer.append("| --- | --- |\n");
        answer.append("| 老人数量 | ").append(data.get("elderCount")).append(" |\n");
        answer.append("| 机构数量 | ").append(data.get("sanaCount")).append(" |\n");
        answer.append("| 护理人员数量 | ").append(data.get("nurseCount")).append(" |\n");
        answer.append("| 医护人员数量 | ").append(data.get("medicineCount")).append(" |\n");
        answer.append("| 床位总数 | ").append(data.get("bedCount")).append(" |\n");
        answer.append("| 已用床位数 | ").append(data.get("bedInUse")).append(" |\n");
        answer.append("| 床位使用率 | ").append(data.get("bedUseRatePercent")).append(" |\n");
        return answer.toString();
    }

    /**
     * 方法：renderRegionDistribution
     *
     * @author zhanghongyu
     */
    private String renderRegionDistribution(Map<String, Object> result) {
        if (!(Boolean.TRUE.equals(result.get("success")))) {
            return renderFailure(result);
        }

        Map<String, Object> data = castMap(result.get("data"));
        List<Map<String, Object>> regions = castMapList(data.get("regions"));

        StringBuilder answer = new StringBuilder();
        answer.append("## 区域养老机构数量分布\n\n");
        answer.append(buildMetaBlock(String.valueOf(data.get("scopeName")), "系统实时统计")).append("\n");
        if ("organization".equals(String.valueOf(data.get("scope")))) {
            answer.append("> 说明：当前账号仅返回本机构权限范围内的区域统计。\n\n");
        }
        answer.append("| 区域 | 机构数量 |\n");
        answer.append("| --- | --- |\n");
        for (Map<String, Object> region : regions) {
            answer.append("| ")
                    .append(defaultText(region.get("regionName")))
                    .append(" | ")
                    .append(defaultText(region.get("count")))
                    .append(" 家 |\n");
        }
        return answer.toString();
    }

    /**
     * 方法：renderSanatoriumDetail
     *
     * @author zhanghongyu
     */
    private String renderSanatoriumDetail(Map<String, Object> result) {
        if (!(Boolean.TRUE.equals(result.get("success")))) {
            return renderFailure(result);
        }

        Map<String, Object> data = castMap(result.get("data"));
        List<Map<String, Object>> items = castMapList(data.get("items"));
        if (!items.isEmpty()) {
            StringBuilder multiAnswer = new StringBuilder();
            multiAnswer.append("## 机构详情统计\n\n");
            multiAnswer.append(buildMetaBlock(defaultText(data.get("scopeName")), "系统实时统计")).append("\n");
            multiAnswer.append("| 机构名称 | 所属区划 | 运营状态 | 老人数量 | 护理人员数量 | 医护人员数量 | 床位总数 | 已用床位数 | 床位使用率 |\n");
            multiAnswer.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
            for (Map<String, Object> item : items) {
                Map<String, Object> itemUseRate = castMap(item.get("bedUseRate"));
                multiAnswer.append("| ")
                        .append(defaultText(item.get("name"))).append(" | ")
                        .append(defaultText(item.get("affiliation"))).append(" | ")
                        .append(defaultText(item.get("status"))).append(" | ")
                        .append(defaultText(item.get("elderCount"))).append(" | ")
                        .append(defaultText(item.get("nursingCount"))).append(" | ")
                        .append(defaultText(item.get("medicalCount"))).append(" | ")
                        .append(defaultText(item.get("bedCount"))).append(" | ")
                        .append(defaultText(item.get("bedInUse"))).append(" | ")
                        .append(defaultText(itemUseRate.get("percent"))).append(" |\n");
            }
            return multiAnswer.toString();
        }

        Map<String, Object> useRate = castMap(data.get("bedUseRate"));

        StringBuilder answer = new StringBuilder();
        answer.append("## 机构详情统计\n\n");
        answer.append(buildMetaBlock(defaultText(data.get("name")), "系统实时统计")).append("\n");
        answer.append("| 字段 | 值 |\n");
        answer.append("| --- | --- |\n");
        answer.append("| 机构名称 | ").append(defaultText(data.get("name"))).append(" |\n");
        answer.append("| 所属区划 | ").append(defaultText(data.get("affiliation"))).append(" |\n");
        answer.append("| 机构地址 | ").append(defaultText(data.get("address"))).append(" |\n");
        answer.append("| 运营状态 | ").append(defaultText(data.get("status"))).append(" |\n");
        answer.append("| 统一社会信用代码 | ").append(defaultText(data.get("uscc"))).append(" |\n");
        answer.append("| 法人姓名 | ").append(defaultText(data.get("legalPerson"))).append(" |\n");
        answer.append("| 法人联系方式 | ").append(defaultText(data.get("legalPhone"))).append(" |\n");
        answer.append("| 老人数量 | ").append(defaultText(data.get("elderCount"))).append(" |\n");
        answer.append("| 护理人员数量 | ").append(defaultText(data.get("nursingCount"))).append(" |\n");
        answer.append("| 医护人员数量 | ").append(defaultText(data.get("medicalCount"))).append(" |\n");
        answer.append("| 床位总数 | ").append(defaultText(data.get("bedCount"))).append(" |\n");
        answer.append("| 已用床位数 | ").append(defaultText(data.get("bedInUse"))).append(" |\n");
        answer.append("| 床位使用率 | ").append(defaultText(useRate.get("percent"))).append(" |\n");
        return answer.toString();
    }

    /**
     * 方法：renderCareLevelDistribution
     *
     * @author zhanghongyu
     */
    private String renderCareLevelDistribution(Map<String, Object> result) {
        if (!(Boolean.TRUE.equals(result.get("success")))) {
            return renderFailure(result);
        }

        Map<String, Object> data = castMap(result.get("data"));
        List<Map<String, Object>> items = castMapList(data.get("items"));
        if (!items.isEmpty()) {
            StringBuilder multiAnswer = new StringBuilder();
            multiAnswer.append("## 老人自理能力分布\n\n");
            multiAnswer.append(buildMetaBlock(String.valueOf(data.get("scopeName")), "系统实时统计")).append("\n");
            for (Map<String, Object> item : items) {
                Map<String, Object> distribution = castMap(item.get("distribution"));
                multiAnswer.append("### ").append(defaultText(item.get("name"))).append("\n\n");
                multiAnswer.append("| 能力等级 | 人数 |\n");
                multiAnswer.append("| --- | --- |\n");
                multiAnswer.append("| 能力完好 | ").append(defaultText(distribution.get("能力完好"))).append(" 人 |\n");
                multiAnswer.append("| 轻度失能 | ").append(defaultText(distribution.get("轻度失能"))).append(" 人 |\n");
                multiAnswer.append("| 中度失能 | ").append(defaultText(distribution.get("中度失能"))).append(" 人 |\n");
                multiAnswer.append("| 重度失能 | ").append(defaultText(distribution.get("重度失能"))).append(" 人 |\n");
                multiAnswer.append("| 完全失能 | ").append(defaultText(distribution.get("完全失能"))).append(" 人 |\n\n");
            }
            return multiAnswer.toString();
        }

        Map<String, Object> distribution = castMap(data.get("distribution"));

        StringBuilder answer = new StringBuilder();
        answer.append("## 老人自理能力分布\n\n");
        answer.append(buildMetaBlock(String.valueOf(data.get("scopeName")), "系统实时统计")).append("\n");
        answer.append("| 能力等级 | 人数 |\n");
        answer.append("| --- | --- |\n");
        answer.append("| 能力完好 | ").append(defaultText(distribution.get("能力完好"))).append(" 人 |\n");
        answer.append("| 轻度失能 | ").append(defaultText(distribution.get("轻度失能"))).append(" 人 |\n");
        answer.append("| 中度失能 | ").append(defaultText(distribution.get("中度失能"))).append(" 人 |\n");
        answer.append("| 重度失能 | ").append(defaultText(distribution.get("重度失能"))).append(" 人 |\n");
        answer.append("| 完全失能 | ").append(defaultText(distribution.get("完全失能"))).append(" 人 |\n");
        return answer.toString();
    }

    private String renderNursingTasks(Map<String, Object> result) {
        if (!(Boolean.TRUE.equals(result.get("success")))) {
            return renderFailure(result);
        }

        Map<String, Object> data = castMap(result.get("data"));
        List<Map<String, Object>> records = castMapList(data.get("records"));

        StringBuilder answer = new StringBuilder();
        answer.append("## 护理任务查询\n\n");
        answer.append(buildMetaBlock(defaultText(data.get("scopeName")), "系统实时统计")).append("\n");
        answer.append("> 命中总数：").append(defaultText(data.get("total"))).append(" 条，当前返回 ")
                .append(records.size()).append(" 条。\n\n");
        if (records.isEmpty()) {
            answer.append("当前未查询到符合条件的护理任务。");
            return answer.toString();
        }
        answer.append("| 任务ID | 机构 | 老人 | 任务标题 | 状态 | 优先级 | 执行人 | 计划开始 | 计划结束 |\n");
        answer.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
        for (Map<String, Object> record : records) {
            answer.append("| ")
                    .append(defaultText(record.get("id"))).append(" | ")
                    .append(defaultText(record.get("sanaName"))).append(" | ")
                    .append(defaultText(record.get("elderName"))).append(" | ")
                    .append(defaultText(record.get("taskTitle"))).append(" | ")
                    .append(defaultText(record.get("statusName"))).append(" | ")
                    .append(defaultText(record.get("priorityName"))).append(" | ")
                    .append(defaultText(record.get("assigneeUsername"))).append(" | ")
                    .append(defaultText(record.get("plannedStartTime"))).append(" | ")
                    .append(defaultText(record.get("plannedEndTime"))).append(" |\n");
        }
        return answer.toString();
    }

    private String renderNursingLogs(Map<String, Object> result) {
        if (!(Boolean.TRUE.equals(result.get("success")))) {
            return renderFailure(result);
        }

        Map<String, Object> data = castMap(result.get("data"));
        List<Map<String, Object>> records = castMapList(data.get("records"));

        StringBuilder answer = new StringBuilder();
        answer.append("## 护理日志查询\n\n");
        answer.append(buildMetaBlock(defaultText(data.get("scopeName")), "系统实时统计")).append("\n");
        answer.append("> 命中总数：").append(defaultText(data.get("total"))).append(" 条，当前返回 ")
                .append(records.size()).append(" 条。\n\n");
        if (records.isEmpty()) {
            answer.append("当前未查询到符合条件的护理日志。");
            return answer.toString();
        }
        answer.append("| 日志ID | 机构 | 老人 | 任务 | 护理人员 | 日志时间 | 状态 | 内容 |\n");
        answer.append("| --- | --- | --- | --- | --- | --- | --- | --- |\n");
        for (Map<String, Object> record : records) {
            answer.append("| ")
                    .append(defaultText(record.get("id"))).append(" | ")
                    .append(defaultText(record.get("sanaName"))).append(" | ")
                    .append(defaultText(record.get("elderName"))).append(" | ")
                    .append(defaultText(record.get("taskTitle"))).append(" | ")
                    .append(defaultText(record.get("nurseUsername"))).append(" | ")
                    .append(defaultText(record.get("logTime"))).append(" | ")
                    .append(defaultText(record.get("abnormalName"))).append(" | ")
                    .append(truncate(defaultText(record.get("content")), 60)).append(" |\n");
        }
        return answer.toString();
    }

    private String renderCombinedResults(List<String> sections) {
        StringBuilder answer = new StringBuilder();
        for (String section : sections) {
            if (StrUtil.isBlank(section)) {
                continue;
            }
            if (!answer.isEmpty()) {
                answer.append("\n\n");
            }
            answer.append(section);
        }
        return answer.toString();
    }

    private Integer resolveTaskStatus(QueryContext context) {
        if (resolveNormalTaskOnly(context) || resolveAbnormalTaskOnly(context)) {
            return null;
        }
        if (hasAny(context, "待执行", "待办", "未开始")) {
            return 0;
        }
        if (hasAny(context, "执行中", "进行中")) {
            return 1;
        }
        if (hasAny(context, "已完成", "完成")) {
            return 2;
        }
        if (hasAny(context, "已取消", "取消")) {
            return 3;
        }
        if (hasAny(context, "已超时", "超时", "逾期")) {
            return 4;
        }
        return null;
    }

    private Boolean resolveNormalTaskOnly(QueryContext context) {
        return hasAny(context, "正常护理任务", "正常任务")
                || (hasAny(context, "正常") && hasAny(context, "护理任务", "任务列表", "任务"));
    }

    private Boolean resolveAbnormalTaskOnly(QueryContext context) {
        return hasAny(context, "异常护理任务", "异常任务", "逾期任务")
                || (hasAny(context, "异常", "逾期") && hasAny(context, "护理任务", "任务列表", "任务"))
                || (hasAny(context, "超时") && hasAny(context, "护理任务", "任务列表", "任务"));
    }

    private Integer resolveAbnormalFlag(QueryContext context) {
        if (hasAny(context, "异常")) {
            return 1;
        }
        if (hasAny(context, "正常", "普通")) {
            return 0;
        }
        return null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * 方法：renderFailure
     *
     * @author zhanghongyu
     */
    private String renderFailure(Map<String, Object> result) {
        String message = defaultText(result.get("message"));
        Object data = result.get("data");
        if (!(data instanceof Map<?, ?> dataMap) || dataMap.isEmpty()) {
            return "## 查询失败\n\n> " + message;
        }

        StringBuilder answer = new StringBuilder("## 查询失败\n\n");
        answer.append("> ").append(message).append("\n\n");
        answer.append("| 字段 | 值 |\n");
        answer.append("| --- | --- |\n");
        dataMap.forEach((key, value) -> answer.append("| ").append(key).append(" | ").append(value).append(" |\n"));
        return answer.toString();
    }

    /**
     * 方法：buildMetaBlock
     *
     * @author zhanghongyu
     */
    private String buildMetaBlock(String scopeName, String source) {
        return new StringBuilder()
                .append("> 统计范围：").append(defaultText(scopeName)).append("  \n")
                .append("> 数据来源：").append(defaultText(source))
                .toString();
    }

    /**
     * 方法：castMap
     *
     * @author zhanghongyu
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        if (value == null) {
            return Map.of();
        }
        return BeanUtil.beanToMap(value, new LinkedHashMap<>(), false, false);
    }

    /**
     * 方法：castMapList
     *
     * @author zhanghongyu
     */
    private List<Map<String, Object>> castMapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            result.add(castMap(item));
        }
        return result;
    }

    /**
     * 方法：defaultText
     *
     * @author zhanghongyu
     */
    private String defaultText(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private enum QueryIntent {
        OVERVIEW,
        REGION_DISTRIBUTION,
        SANATORIUM_DETAIL,
        CARE_LEVEL_DISTRIBUTION,
        NURSING_TASKS,
        NURSING_LOGS
    }

    private record QueryContext(
            String originalPrompt,
            String compactPrompt,
            String sanaName,
            boolean explicitGlobal,
            boolean explicitCurrentOrg
    ) {
    }
}
