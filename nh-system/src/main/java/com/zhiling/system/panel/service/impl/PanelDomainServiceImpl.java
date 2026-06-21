package com.zhiling.system.panel.service.impl;

import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.model.entity.Sanatorium;
import com.zhiling.model.vo.PanelNavInfo;
import com.zhiling.model.vo.RegionSanaCountVo;
import com.zhiling.system.panel.service.PanelDomainService;
import com.zhiling.system.panel.service.PanelStatisticsQueryService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 大屏域服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class PanelDomainServiceImpl implements PanelDomainService {
    private static final String PANEL_NAV_PATH = "/web/panel/getNavDistribution";
    private static final String PANEL_REGION_PATH = "/web/panel/regionSanaCount";
    private static final String PANEL_EXPORT_REGION_PATH = "/web/panel/exportRegionStats";

    private final PanelStatisticsQueryService panelStatisticsQueryService;
    private final SecurityHelper securityHelper;

    public PanelDomainServiceImpl(PanelStatisticsQueryService panelStatisticsQueryService,
                                   SecurityHelper securityHelper) {
        this.panelStatisticsQueryService = panelStatisticsQueryService;
        this.securityHelper = securityHelper;
    }

    /**
     * 方法：getNavDistribution
     *
     * @author zhanghongyu
     */
    @Override
    public PanelNavInfo getNavDistribution() {
        requireResource(PANEL_NAV_PATH);
        if (!securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            return buildOrgPanelNavInfo();
        }

        Integer elderCount = panelStatisticsQueryService.elderCount();
        Integer sanatoriumCount = panelStatisticsQueryService.sanatoriumCount();
        Integer nurseCount = panelStatisticsQueryService.nurseCount();
        Integer medicalCount = panelStatisticsQueryService.medicineCount();
        Integer affiliationCount = panelStatisticsQueryService.affiliationCount();
        Integer bedCount = panelStatisticsQueryService.bedCount();
        Integer bedInUse = panelStatisticsQueryService.useBedCount();

        elderCount = elderCount == null ? 0 : elderCount;
        sanatoriumCount = sanatoriumCount == null ? 0 : sanatoriumCount;
        nurseCount = nurseCount == null ? 0 : nurseCount;
        medicalCount = medicalCount == null ? 0 : medicalCount;
        affiliationCount = affiliationCount == null ? 0 : affiliationCount;
        bedCount = bedCount == null ? 0 : bedCount;
        bedInUse = bedInUse == null ? 0 : bedInUse;

        Double useRate = bedCount == 0 ? 0D : (1.0 * bedInUse / bedCount);
        return PanelNavInfo.builder()
                .elderCount(elderCount)
                .sanaCount(sanatoriumCount)
                .nurseCount(nurseCount)
                .medicineCount(medicalCount)
                .affiliationCount(affiliationCount)
                .useRate(useRate)
                .build();
    }

    /**
     * 方法：regionSanaCount
     *
     * @author zhanghongyu
     */
    @Override
    public List<RegionSanaCountVo> regionSanaCount() {
        requireResource(PANEL_REGION_PATH);
        return regionSanaCountInternal();
    }

    private List<RegionSanaCountVo> regionSanaCountInternal() {
        if (securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            return panelStatisticsQueryService.regionSanaCount();
        }
        Set<Long> sanaScopeIds = securityHelper.getCurrentSanaScopeIdsForSensitiveOperation();
        if (sanaScopeIds == null || sanaScopeIds.isEmpty()) {
            return buildEmptyRegionStats();
        }
        List<Sanatorium> sanatoriumList = panelStatisticsQueryService.listSanatoriumByIds(sanaScopeIds);
        return buildRegionStatsBySanatoriums(sanatoriumList);
    }

    /**
     * 方法：exportRegionStats
     *
     * @author zhanghongyu
     */
    @Override
    public List<RegionSanaCountVo> exportRegionStats() {
        requireResource(PANEL_EXPORT_REGION_PATH);
        return regionSanaCountInternal();
    }

    /**
     * 方法：buildOrgPanelNavInfo
     *
     * @author zhanghongyu
     */
    private PanelNavInfo buildOrgPanelNavInfo() {
        Set<Long> sanaScopeIds = securityHelper.getCurrentSanaScopeIdsForSensitiveOperation();
        if (sanaScopeIds == null || sanaScopeIds.isEmpty()) {
            return emptyPanelNavInfo();
        }

        List<Sanatorium> sanatoriumList = panelStatisticsQueryService.listSanatoriumByIds(sanaScopeIds);
        if (sanatoriumList == null || sanatoriumList.isEmpty()) {
            return emptyPanelNavInfo();
        }

        Integer elderCount = panelStatisticsQueryService.elderCountBySanaIds(sanaScopeIds);
        int nurseCount = 0;
        int medicalCount = 0;
        int bedCount = 0;
        int bedInUse = 0;
        Set<String> affiliationSet = new LinkedHashSet<>();
        for (Sanatorium sanatorium : sanatoriumList) {
            nurseCount += sanatorium.getNursingCount() == null ? 0 : sanatorium.getNursingCount();
            medicalCount += sanatorium.getMedicalCount() == null ? 0 : sanatorium.getMedicalCount();
            bedCount += sanatorium.getBedCount() == null ? 0 : sanatorium.getBedCount();
            bedInUse += sanatorium.getBedInUse() == null ? 0 : sanatorium.getBedInUse();
            if (sanatorium.getSanaAffiliation() != null && !sanatorium.getSanaAffiliation().trim().isEmpty()) {
                affiliationSet.add(sanatorium.getSanaAffiliation().trim());
            }
        }

        elderCount = elderCount == null ? 0 : elderCount;
        Double useRate = bedCount == 0 ? 0D : (1.0 * bedInUse / bedCount);

        return PanelNavInfo.builder()
                .elderCount(elderCount)
                .sanaCount(sanatoriumList.size())
                .nurseCount(nurseCount)
                .medicineCount(medicalCount)
                .affiliationCount(Math.max(affiliationSet.size(), 1))
                .useRate(useRate)
                .build();
    }

    /**
     * 方法：emptyPanelNavInfo
     *
     * @author zhanghongyu
     */
    private PanelNavInfo emptyPanelNavInfo() {
        return PanelNavInfo.builder()
                .elderCount(0)
                .sanaCount(0)
                .nurseCount(0)
                .medicineCount(0)
                .affiliationCount(0)
                .useRate(0D)
                .build();
    }

    /**
     * 方法：buildEmptyRegionStats
     *
     * @author zhanghongyu
     */
    private List<RegionSanaCountVo> buildEmptyRegionStats() {
        return buildRegionStatsBySanatoriums(List.of());
    }

    /**
     * 方法：buildRegionStatsBySanatoriums
     *
     * @author zhanghongyu
     */
    private List<RegionSanaCountVo> buildRegionStatsBySanatoriums(List<Sanatorium> sanatoriumList) {
        String[] regions = {"德城区", "陵城区", "宁津县", "庆云县", "临邑县", "齐河县", "平原县", "夏津县", "武城县", "乐陵市", "禹城市"};
        Map<String, Long> regionCountMap = new LinkedHashMap<>();
        for (String region : regions) {
            regionCountMap.put(region, 0L);
        }
        if (sanatoriumList != null) {
            for (Sanatorium sanatorium : sanatoriumList) {
                String region = resolveRegionName(sanatorium);
                if (region != null && regionCountMap.containsKey(region)) {
                    regionCountMap.put(region, regionCountMap.get(region) + 1);
                }
            }
        }
        List<RegionSanaCountVo> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : regionCountMap.entrySet()) {
            result.add(RegionSanaCountVo.builder()
                    .regionName(entry.getKey())
                    .sanaCount(entry.getValue())
                    .build());
        }
        return result;
    }

    /**
     * 方法：resolveRegionName
     *
     * @author zhanghongyu
     */
    private String resolveRegionName(Sanatorium sanatorium) {
        if (sanatorium == null) {
            return null;
        }
        String source = String.join("",
                safeText(sanatorium.getSanaAffiliation()),
                safeText(sanatorium.getSanaAddress()),
                safeText(sanatorium.getSanaName()));
        if (source.contains("德城区") || source.contains("开发区") || source.contains("本级")) return "德城区";
        if (source.contains("陵城区")) return "陵城区";
        if (source.contains("宁津")) return "宁津县";
        if (source.contains("庆云")) return "庆云县";
        if (source.contains("临邑")) return "临邑县";
        if (source.contains("齐河")) return "齐河县";
        if (source.contains("平原")) return "平原县";
        if (source.contains("夏津")) return "夏津县";
        if (source.contains("武城")) return "武城县";
        if (source.contains("乐陵")) return "乐陵市";
        if (source.contains("禹城")) return "禹城市";
        return null;
    }

    /**
     * 方法：safeText
     *
     * @author zhanghongyu
     */
    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void requireResource(String resourcePath) {
        if (!securityHelper.hasResourcePathForSensitiveOperation(resourcePath)) {
            throw new AccessDeniedException("无权访问大屏统计功能");
        }
    }
}
