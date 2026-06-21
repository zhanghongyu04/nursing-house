package com.zhiling.agent.infrastructure.mcp;

import cn.hutool.core.util.StrUtil;
import com.zhiling.framework.system.port.ElderQueryPort;
import com.zhiling.framework.system.port.NursingCareQueryPort;
import com.zhiling.framework.system.port.PanelStatisticsPort;
import com.zhiling.framework.system.port.SanatoriumQueryPort;
import com.zhiling.framework.system.model.ElderCareLevelStats;
import com.zhiling.framework.system.model.NursingLogQueryResult;
import com.zhiling.framework.system.model.NursingTaskQueryResult;
import com.zhiling.framework.system.model.SanatoriumDetailStats;
import com.zhiling.framework.system.model.SanatoriumSummary;
import com.zhiling.framework.system.model.SystemOverviewStats;
import com.zhiling.framework.system.model.RegionSanatoriumCount;
import com.zhiling.framework.security.CurrentUserProvider;
import com.zhiling.framework.security.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 提供给智能体调用的系统内部数据 MCP 工具集。
 *
 * 本类通过 framework 公开契约访问 system 数据，不再直接依赖 system 内部实现。
 *
 * @author zhanghongyu
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentInternalDataMcp {

    // Tool 结果集分页上限，避免 MCP 被一次性大结果拖慢。
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;
    private static final String TOOL_SYSTEM_OVERVIEW = "get_system_overview_stats";
    private static final String TOOL_REGION_DISTRIBUTION = "get_region_sanatorium_distribution";
    private static final String TOOL_SEARCH_SANATORIUM = "search_sanatoriums_by_keyword";
    private static final String TOOL_SANATORIUM_DETAIL = "get_sanatorium_detail_stats";
    private static final String TOOL_ELDER_CARE_LEVEL = "get_elder_care_level_stats";
    private static final String TOOL_NURSING_TASKS = "list_nursing_tasks";
    private static final String TOOL_NURSING_LOGS = "list_nursing_logs";

    private final PanelStatisticsPort panelStatisticsPort;
    private final SanatoriumQueryPort sanatoriumQueryPort;
    private final ElderQueryPort elderQueryPort;
    private final NursingCareQueryPort nursingCareQueryPort;
    private final SecurityHelper securityHelper;

    /**
     * 系统概览：
     * 1) 政府管理员读取全局聚合；
     * 2) 机构侧账号按授权机构范围聚合；
     * 3) 返回统一 success/message/data 包装，便于智能体稳定消费。
     */
    @Tool(name = "get_system_overview_stats", description = "查询当前账号有权访问范围内的系统概览统计，包括老人数量、机构数量、护理人员数量、医护人员数量、床位总数、已用床位数和床位使用率。用户询问平台总体情况、系统概览、当前机构概况、床位总体使用情况、老人/机构/护理人员/医护人员总数时应调用。")
    public Map<String, Object> getSystemOverviewStats() {
        SystemOverviewStats stats = panelStatisticsPort.getSystemOverviewStats();

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("scope", stats.getScope());
        data.put("scopeName", stats.getScopeName());
        data.put("sanaCount", stats.getSanaCount());
        data.put("elderCount", stats.getElderCount());
        data.put("nurseCount", stats.getNurseCount());
        data.put("medicineCount", stats.getMedicineCount());
        data.put("affiliationCount", stats.getAffiliationCount());
        data.put("bedCount", stats.getBedCount());
        data.put("bedInUse", stats.getBedInUse());
        data.put("bedUseRate", stats.getBedUseRate());
        data.put("bedUseRatePercent", stats.getBedUseRatePercent());

        return success(TOOL_SYSTEM_OVERVIEW, "已返回系统概览统计。", data);
    }

    /**
     * 方法：getRegionSanatoriumDistribution
     *
     * @author zhanghongyu
     */
    @Tool(name = "get_region_sanatorium_distribution", description = "查询当前账号有权访问范围内的区域养老机构数量分布。用户询问地图数据、区域机构数、区县养老机构数量、各区县机构分布、行政区分布时应调用。")
    public Map<String, Object> getRegionSanatoriumDistribution() {
        List<RegionSanatoriumCount> regions = panelStatisticsPort.getRegionSanatoriumDistribution();
        boolean govAdmin = securityHelper.hasGovAdminRoleForSensitiveOperation();
        int scopeSize = securityHelper.getCurrentSanaScopeIdsForSensitiveOperation().size();

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("scope", govAdmin ? "global" : "organization");
        data.put("scopeName", govAdmin ? "全局" : (scopeSize > 1 ? "多机构范围" : "当前机构"));
        data.put("regions", regions);

        return success(TOOL_REGION_DISTRIBUTION, "已返回区域养老机构数量分布。", data);
    }

    @Tool(name = "search_sanatoriums_by_keyword", description = "按机构名称关键字检索当前账号有权访问的养老机构。用户提到不完整机构名、模糊机构名、需要先找机构候选，或指定机构详情但名称不精确时应先调用。")
    public Map<String, Object> searchSanatoriumsByKeyword(
            @ToolParam(description = "机构名称关键字", required = true) String keyword,
            @ToolParam(description = "返回数量上限，默认5，最大10", required = false) Integer limit) {
        if (StrUtil.isBlank(keyword)) {
            return failure(TOOL_SEARCH_SANATORIUM, "机构关键字不能为空。");
        }

        int safeLimit = normalizeLimit(limit);
        List<SanatoriumSummary> summaries = sanatoriumQueryPort.searchByKeyword(keyword, safeLimit);

        List<Map<String, Object>> items = summaries.stream()
                .map(this::buildSanatoriumSummary)
                .toList();

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("keyword", keyword);
        data.put("count", items.size());
        data.put("items", items);
        return success(TOOL_SEARCH_SANATORIUM, "已返回机构检索结果。", data);
    }

    /**
     * 方法：buildSanatoriumSummary
     *
     * @author zhanghongyu
     */
    private Map<String, Object> buildSanatoriumSummary(SanatoriumSummary summary) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("id", summary.getId());
        item.put("name", summary.getSanaName());
        item.put("affiliation", summary.getRegionName());
        item.put("address", summary.getAddress());
        item.put("status", summary.getStatus());
        item.put("bedCount", summary.getBedCount());
        item.put("bedInUse", summary.getBedInUse());
        return item;
    }

    @Tool(name = "get_sanatorium_detail_stats", description = "查询某个养老机构的详细信息，包括所属区划、地址、运营状态、统一社会信用代码、法人姓名、法人联系方式、老人数量、护理人员数量、医护人员数量、床位总数、已用床位数和床位使用率。用户询问机构详情、机构画像、地址、法人、信用代码、运营状态、指定机构床位/人员统计时应调用；机构管理员和护理用户仅能查询本机构。")
    public Map<String, Object> getSanatoriumDetailStats(
            @ToolParam(description = "机构名称。政府管理员可传指定机构名称；机构管理员和护理用户可不传或仅查询本机构。", required = false) String sanaName) {
        if (StrUtil.isBlank(sanaName)) {
            List<SanatoriumDetailStats> detailStats = sanatoriumQueryPort.listCurrentScopeDetailStats();
            if (detailStats.isEmpty()) {
                return failure(TOOL_SANATORIUM_DETAIL, "当前账号未绑定养老机构，无法查询。");
            }
            if (detailStats.size() == 1) {
                return success(TOOL_SANATORIUM_DETAIL, "已返回当前机构详情统计。", buildSanatoriumDetail(detailStats.get(0)));
            }
            LinkedHashMap<String, Object> data = new LinkedHashMap<>();
            data.put("scope", "organization");
            data.put("scopeName", "多机构范围");
            data.put("count", detailStats.size());
            data.put("items", detailStats.stream().map(this::buildSanatoriumDetail).toList());
            return success(TOOL_SANATORIUM_DETAIL, "已返回当前账号可访问的多机构详情统计。", data);
        }

        SanatoriumDetailStats detailStats = sanatoriumQueryPort.getDetailStats(sanaName);
        if (detailStats == null) {
            return failure(TOOL_SANATORIUM_DETAIL, "未找到匹配的养老机构。", Map.of("keyword", sanaName));
        }
        return success(TOOL_SANATORIUM_DETAIL, "已返回指定机构详情统计。", buildSanatoriumDetail(detailStats));
    }

    /**
     * 方法：buildSanatoriumDetail
     *
     * @author zhanghongyu
     */
    private Map<String, Object> buildSanatoriumDetail(SanatoriumDetailStats stats) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("id", stats.getId());
        item.put("name", stats.getSanaName());
        item.put("affiliation", stats.getRegionName());
        item.put("address", stats.getAddress());
        item.put("status", stats.getStatus());
        item.put("elderCount", stats.getElderCount());
        item.put("nursingCount", stats.getNurseCount());
        item.put("medicineCount", stats.getMedicineCount());
        item.put("bedCount", stats.getBedCount());
        item.put("bedInUse", stats.getBedInUse());
        item.put("bedUseRate", buildUseRateMap(stats.getBedUseRate(), stats.getBedUseRatePercent()));
        item.put("uscc", stats.getUscc());
        item.put("legalPersons", stats.getLegalPersons());
        item.put("legalPhone", stats.getLegalPhone());
        return item;
    }

    /**
     * 方法：buildUseRateMap
     *
     * @author zhanghongyu
     */
    private Map<String, Object> buildUseRateMap(Double value, String percent) {
        LinkedHashMap<String, Object> useRate = new LinkedHashMap<>();
        useRate.put("value", value);
        useRate.put("percent", percent);
        return useRate;
    }

    @Tool(name = "get_elder_care_level_stats", description = "查询老人自理能力分布统计，可回答能力完好、轻度失能、中度失能、重度失能、完全失能等人数。用户询问老人自理能力、能力等级、失能等级、能力分布、失能分布时应调用；政府管理员不传机构名称时返回全局统计，机构管理员和护理用户仅能查询本机构。")
    public Map<String, Object> getElderCareLevelStats(
            @ToolParam(description = "机构名称。政府管理员可选填，不填则查询全局；机构管理员和护理用户可不传或仅查询本机构。", required = false) String sanaName) {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();

        if (StrUtil.isBlank(sanaName)) {
            List<ElderCareLevelStats> currentScopeStats = elderQueryPort.listCurrentScopeCareLevelStats();
            if (currentScopeStats != null && !currentScopeStats.isEmpty()) {
                if (currentScopeStats.size() == 1) {
                    ElderCareLevelStats stats = currentScopeStats.get(0);
                    data.put("scope", stats.getScope());
                    data.put("scopeName", stats.getScopeName());
                    data.put("distribution", stats.getDistribution());
                    return success(TOOL_ELDER_CARE_LEVEL, "已返回当前机构老人自理能力分布。", data);
                }
                data.put("scope", "organization");
                data.put("scopeName", "多机构范围");
                data.put("count", currentScopeStats.size());
                data.put("items", currentScopeStats);
                return success(TOOL_ELDER_CARE_LEVEL, "已返回当前账号可访问的多机构老人自理能力分布。", data);
            }

            ElderCareLevelStats stats = elderQueryPort.getCareLevelStats(null);
            if (stats == null) {
                return failure(TOOL_ELDER_CARE_LEVEL, "当前账号未绑定养老机构，无法查询。");
            }
            data.put("scope", stats.getScope());
            data.put("scopeName", stats.getScopeName());
            data.put("distribution", stats.getDistribution());
            return success(TOOL_ELDER_CARE_LEVEL, "已返回当前机构老人自理能力分布。", data);
        }

        ElderCareLevelStats stats = elderQueryPort.getCareLevelStats(sanaName);
        if (stats == null) {
            return failure(TOOL_ELDER_CARE_LEVEL, "未找到匹配的养老机构。", Map.of("keyword", sanaName));
        }
        data.put("scope", stats.getScope());
        data.put("scopeName", stats.getScopeName());
        data.put("distribution", stats.getDistribution());
        return success(TOOL_ELDER_CARE_LEVEL, "已返回指定机构老人自理能力分布。", data);
    }

    @Tool(name = "list_nursing_tasks", description = "查询当前账号有权访问范围内的护理任务列表，可按机构名称和任务状态筛选。状态：0待执行、1执行中、2已完成、3已取消、4已超时；normalOnly=true 表示查询正常护理任务，即待执行和执行中，排除已完成、已取消、已超时；abnormalOnly=true 表示查询异常护理任务，即已超时/逾期任务。用户询问护理任务、正常任务、异常任务、待办任务、已完成任务、超时任务、逾期任务时应调用。")
    public Map<String, Object> listNursingTasks(
            @ToolParam(description = "机构名称。机构侧账号不传则查询当前机构范围；多机构账号可传指定机构名称。", required = false) String sanaName,
            @ToolParam(description = "任务状态：0待执行、1执行中、2已完成、3已取消、4已超时。不传则返回全部状态。", required = false) Integer status,
            @ToolParam(description = "是否只查询正常护理任务。true 表示查询待执行和执行中任务，并排除已完成、已取消、已超时。", required = false) Boolean normalOnly,
            @ToolParam(description = "是否只查询异常护理任务。true 表示查询已超时或逾期任务，对应 status=4。", required = false) Boolean abnormalOnly,
            @ToolParam(description = "返回数量上限，默认5，最大10", required = false) Integer limit) {
        int safeLimit = normalizeLimit(limit);
        Set<Integer> statuses = resolveTaskStatuses(status, normalOnly, abnormalOnly);
        NursingTaskQueryResult queryResult = nursingCareQueryPort.listNursingTasks(sanaName, statuses, safeLimit);

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("scope", queryResult.getScope());
        data.put("scopeName", queryResult.getScopeName());
        data.put("status", status);
        data.put("normalOnly", Boolean.TRUE.equals(normalOnly));
        data.put("abnormalOnly", Boolean.TRUE.equals(abnormalOnly));
        data.put("statuses", statuses);
        data.put("total", queryResult.getTotal());
        data.put("limit", safeLimit);
        data.put("records", queryResult.getRecords());

        if (queryResult.getRecords() == null || queryResult.getRecords().isEmpty()) {
            return success(TOOL_NURSING_TASKS, "当前未查询到符合条件的护理任务。", data);
        }
        return success(TOOL_NURSING_TASKS, "已返回护理任务列表。", data);
    }

    private Set<Integer> resolveTaskStatuses(Integer status, Boolean normalOnly, Boolean abnormalOnly) {
        if (Boolean.TRUE.equals(abnormalOnly)) {
            return Set.of(4);
        }
        if (Boolean.TRUE.equals(normalOnly)) {
            return Set.of(0, 1);
        }
        if (status == null) {
            return null;
        }
        return Set.of(status);
    }

    @Tool(name = "list_nursing_logs", description = "查询当前账号有权访问范围内的护理日志列表，可按机构名称和是否异常筛选。abnormalFlag：0正常/普通、1异常。用户询问护理日志、护理记录、正常日志、普通日志、异常日志、护理执行记录、护理过程记录时应调用；正常护理日志/普通护理日志传 abnormalFlag=0，异常护理日志传 abnormalFlag=1。")
    public Map<String, Object> listNursingLogs(
            @ToolParam(description = "机构名称。机构侧账号不传则查询当前机构范围；多机构账号可传指定机构名称。", required = false) String sanaName,
            @ToolParam(description = "是否异常：0正常、1异常。不传则返回全部日志。", required = false) Integer abnormalFlag,
            @ToolParam(description = "返回数量上限，默认5，最大10", required = false) Integer limit) {
        int safeLimit = normalizeLimit(limit);
        NursingLogQueryResult queryResult = nursingCareQueryPort.listNursingLogs(sanaName, abnormalFlag, safeLimit);

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("scope", queryResult.getScope());
        data.put("scopeName", queryResult.getScopeName());
        data.put("abnormalFlag", abnormalFlag);
        data.put("total", queryResult.getTotal());
        data.put("limit", safeLimit);
        data.put("records", queryResult.getRecords());

        if (queryResult.getRecords() == null || queryResult.getRecords().isEmpty()) {
            return success(TOOL_NURSING_LOGS, "当前未查询到符合条件的护理日志。", data);
        }
        return success(TOOL_NURSING_LOGS, "已返回护理日志列表。", data);
    }

    /**
     * 统一约束 MCP 查询的返回数量，避免一次调用拉取过多候选结果。
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 方法：success
     *
     * @author zhanghongyu
     */
    private Map<String, Object> success(String toolName, String message, Object data) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", message);
        result.put("data", data);
        // 统一在出口记录成功日志，便于按 toolName 观测 MCP 调用情况。
        logToolSuccess(toolName, message, data);
        return result;
    }

    /**
     * 方法：logToolSuccess
     *
     * @author zhanghongyu
     */
    private void logToolSuccess(String toolName, String message, Object data) {
        String userId = resolveUserId();
        log.info("[MCP][SUCCESS] tool={} userId={} message={} summary={}",
                toolName,
                userId,
                message,
                summarizeData(data));
    }

    /**
     * 方法：failure
     *
     * @author zhanghongyu
     */
    private Map<String, Object> failure(String toolName, String message) {
        return failure(toolName, message, null);
    }

    /**
     * 方法：failure
     *
     * @author zhanghongyu
     */
    private Map<String, Object> failure(String toolName, String message, Object data) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        if (data != null) {
            result.put("data", data);
        }
        // 统一在出口记录失败日志，便于排查参数问题与权限问题。
        logToolFailure(toolName, message, data);
        return result;
    }

    /**
     * 方法：logToolFailure
     *
     * @author zhanghongyu
     */
    private void logToolFailure(String toolName, String message, Object data) {
        String userId = resolveUserId();
        log.warn("[MCP][FAIL] tool={} userId={} message={} summary={}",
                toolName,
                userId,
                message,
                summarizeData(data));
    }

    /**
     * 方法：resolveUserId
     *
     * @author zhanghongyu
     */
    private String resolveUserId() {
        try {
            Long currentUserId = securityHelper.getCurrentUserId();
            if (currentUserId != null) {
                return String.valueOf(currentUserId);
            }
        } catch (Exception ignored) {
            // no-op
        }
        return "unknown";
    }

    /**
     * 方法：summarizeData
     *
     * @author zhanghongyu
     */
    private String summarizeData(Object data) {
        if (data == null) {
            return "null";
        }
        if (data instanceof Map<?, ?> map) {
            return "mapKeys=" + map.keySet();
        }
        if (data instanceof List<?> list) {
            return "listSize=" + list.size();
        }
        return String.valueOf(data);
    }
}
