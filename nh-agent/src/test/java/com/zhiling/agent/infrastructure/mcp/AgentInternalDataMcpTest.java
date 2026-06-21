package com.zhiling.agent.infrastructure.mcp;

import com.zhiling.common.constant.RoleConstant;
import com.zhiling.common.context.UserThreadLocal;
import com.zhiling.common.security.LoginVo;
import com.zhiling.framework.system.port.ElderQueryPort;
import com.zhiling.framework.system.port.NursingCareQueryPort;
import com.zhiling.framework.system.port.PanelStatisticsPort;
import com.zhiling.framework.system.port.SanatoriumQueryPort;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.framework.system.model.ElderCareLevelStats;
import com.zhiling.framework.system.model.NursingLogQueryResult;
import com.zhiling.framework.system.model.NursingLogRecord;
import com.zhiling.framework.system.model.NursingTaskQueryResult;
import com.zhiling.framework.system.model.NursingTaskRecord;
import com.zhiling.framework.system.model.SanatoriumDetailStats;
import com.zhiling.framework.system.model.SanatoriumSummary;
import com.zhiling.framework.system.model.SystemOverviewStats;
import com.zhiling.framework.system.model.RegionSanatoriumCount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MCP 内部数据工具测试类
 *
 * @author zhanghongyu
 */
@ExtendWith(MockitoExtension.class)
class AgentInternalDataMcpTest {

    @Mock
    private PanelStatisticsPort panelStatisticsPort;
    @Mock
    private SanatoriumQueryPort sanatoriumQueryPort;
    @Mock
    private ElderQueryPort elderQueryPort;
    @Mock
    private NursingCareQueryPort nursingCareQueryPort;
    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private AgentInternalDataMcp agentInternalDataMcp;

    @AfterEach
    void cleanThreadLocal() {
        UserThreadLocal.remove();
    }

    @Test
    void getSanatoriumDetailStats_shouldReturnMultiOrgItems_whenOrgAdminHasMultiScopesAndNameBlank() {
        setLoginContext(Set.of(RoleConstant.ORG_ADMIN), Set.of(149L, 150L), 149L);

        SanatoriumDetailStats stats1 = SanatoriumDetailStats.builder()
                .id(149L)
                .sanaName("机构A")
                .regionName("陵城区")
                .bedCount(100)
                .bedInUse(30)
                .build();
        SanatoriumDetailStats stats2 = SanatoriumDetailStats.builder()
                .id(150L)
                .sanaName("机构B")
                .regionName("德城区")
                .bedCount(80)
                .bedInUse(20)
                .build();

        when(sanatoriumQueryPort.listCurrentScopeDetailStats()).thenReturn(List.of(stats1, stats2));

        Map<String, Object> result = agentInternalDataMcp.getSanatoriumDetailStats(" ");

        assertEquals(Boolean.TRUE, result.get("success"));
        assertTrue(String.valueOf(result.get("message")).contains("多机构"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals("organization", data.get("scope"));
        assertEquals("多机构范围", data.get("scopeName"));
        assertEquals(2, data.get("count"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
        assertEquals(2, items.size());
        assertEquals("机构A", items.get(0).get("name"));
        assertEquals("机构B", items.get(1).get("name"));

        verify(sanatoriumQueryPort).listCurrentScopeDetailStats();
    }

    @Test
    void getRegionSanatoriumDistribution_shouldReturnGlobalScope_whenGovAdmin() {
        setLoginContext(Set.of(RoleConstant.GOV_ADMIN), Set.of(), null);
        when(securityHelper.hasGovAdminRoleForSensitiveOperation()).thenReturn(true);
        when(securityHelper.getCurrentSanaScopeIdsForSensitiveOperation()).thenReturn(Set.of());

        when(panelStatisticsPort.getRegionSanatoriumDistribution()).thenReturn(List.of(
                RegionSanatoriumCount.builder().regionName("德城区").count(3).build()
        ));

        Map<String, Object> result = agentInternalDataMcp.getRegionSanatoriumDistribution();

        assertEquals(Boolean.TRUE, result.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals("global", data.get("scope"));
        assertEquals("全局", data.get("scopeName"));

        @SuppressWarnings("unchecked")
        List<RegionSanatoriumCount> regions = (List<RegionSanatoriumCount>) data.get("regions");
        assertEquals(1, regions.size());
        assertEquals(3, regions.get(0).getCount());
    }

    @Test
    void getElderCareLevelStats_shouldReturnMultiOrgItems_whenOrgAdminHasMultiScopesAndNameBlank() {
        when(elderQueryPort.listCurrentScopeCareLevelStats()).thenReturn(List.of(
                ElderCareLevelStats.builder()
                        .scope("organization")
                        .scopeName("机构A")
                        .distribution(Map.of("能力完好", 3))
                        .build(),
                ElderCareLevelStats.builder()
                        .scope("organization")
                        .scopeName("机构B")
                        .distribution(Map.of("能力完好", 5))
                        .build()
        ));

        Map<String, Object> result = agentInternalDataMcp.getElderCareLevelStats(null);

        assertEquals(Boolean.TRUE, result.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals("organization", data.get("scope"));
        assertEquals("多机构范围", data.get("scopeName"));
        assertEquals(2, data.get("count"));
        @SuppressWarnings("unchecked")
        List<ElderCareLevelStats> items = (List<ElderCareLevelStats>) data.get("items");
        assertEquals("机构A", items.get(0).getScopeName());
        assertEquals("机构B", items.get(1).getScopeName());
    }

    @Test
    void listNursingTasks_shouldReturnTaskRecords() {
        NursingTaskRecord record = NursingTaskRecord.builder()
                .id(1L)
                .sanaName("机构A")
                .elderName("张三")
                .taskTitle("晨间护理")
                .statusName("待执行")
                .build();
        when(nursingCareQueryPort.listNursingTasks(null, Set.of(0), 5)).thenReturn(
                NursingTaskQueryResult.builder()
                        .scope("organization")
                        .scopeName("机构A")
                        .total(1L)
                        .records(List.of(record))
                        .build()
        );

        Map<String, Object> result = agentInternalDataMcp.listNursingTasks(null, 0, null, null, 5);

        assertEquals(Boolean.TRUE, result.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals(1L, data.get("total"));
        @SuppressWarnings("unchecked")
        List<NursingTaskRecord> records = (List<NursingTaskRecord>) data.get("records");
        assertEquals("晨间护理", records.get(0).getTaskTitle());
    }

    @Test
    void listNursingTasks_shouldMapNormalOnlyToActiveStatuses() {
        when(nursingCareQueryPort.listNursingTasks(null, Set.of(0, 1), 5)).thenReturn(
                NursingTaskQueryResult.builder()
                        .scope("organization")
                        .scopeName("机构A")
                        .total(0L)
                        .records(List.of())
                        .build()
        );

        Map<String, Object> result = agentInternalDataMcp.listNursingTasks(null, null, true, null, 5);

        assertEquals(Boolean.TRUE, result.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals(Boolean.TRUE, data.get("normalOnly"));
        assertEquals(Set.of(0, 1), data.get("statuses"));
    }

    @Test
    void listNursingTasks_shouldMapAbnormalOnlyToTimeoutStatus() {
        when(nursingCareQueryPort.listNursingTasks(null, Set.of(4), 5)).thenReturn(
                NursingTaskQueryResult.builder()
                        .scope("organization")
                        .scopeName("机构A")
                        .total(0L)
                        .records(List.of())
                        .build()
        );

        Map<String, Object> result = agentInternalDataMcp.listNursingTasks(null, null, null, true, 5);

        assertEquals(Boolean.TRUE, result.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals(Boolean.TRUE, data.get("abnormalOnly"));
        assertEquals(Set.of(4), data.get("statuses"));
    }

    @Test
    void listNursingLogs_shouldReturnLogRecords() {
        NursingLogRecord record = NursingLogRecord.builder()
                .id(2L)
                .sanaName("机构A")
                .elderName("李四")
                .taskTitle("用药护理")
                .nurseUsername("nurse1")
                .abnormalName("异常")
                .content("老人血压偏高")
                .build();
        when(nursingCareQueryPort.listNursingLogs("机构A", 1, 5)).thenReturn(
                NursingLogQueryResult.builder()
                        .scope("organization")
                        .scopeName("机构A")
                        .total(1L)
                        .records(List.of(record))
                        .build()
        );

        Map<String, Object> result = agentInternalDataMcp.listNursingLogs("机构A", 1, 5);

        assertEquals(Boolean.TRUE, result.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals(1L, data.get("total"));
        @SuppressWarnings("unchecked")
        List<NursingLogRecord> records = (List<NursingLogRecord>) data.get("records");
        assertEquals("老人血压偏高", records.get(0).getContent());
    }

    private void setLoginContext(Set<String> roleLabels, Set<Long> sanaScopeIds, Long sanaId) {
        LoginVo loginVo = new LoginVo();
        loginVo.setId(10L);
        loginVo.setUsername("user1");
        loginVo.setRoleLabels(roleLabels);
        loginVo.setSanaScopeIds(sanaScopeIds);
        loginVo.setSanaId(sanaId);
        UserThreadLocal.set(loginVo);
    }
}
