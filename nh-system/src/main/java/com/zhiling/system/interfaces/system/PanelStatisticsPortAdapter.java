package com.zhiling.system.interfaces.system;

import com.zhiling.framework.system.model.RegionSanatoriumCount;
import com.zhiling.framework.system.model.SystemOverviewStats;
import com.zhiling.framework.system.port.PanelStatisticsPort;
import com.zhiling.model.entity.Sanatorium;
import com.zhiling.model.vo.PanelNavInfo;
import com.zhiling.system.infrastructure.persistence.query.SanatoriumQueryService;
import com.zhiling.system.panel.service.PanelDomainService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * system 对外统计查询公开契约适配器。
 *
 * 适配 port 接口，委托持久化查询服务执行查询。
 *
 * @author zhanghongyu
 */
@Component
public class PanelStatisticsPortAdapter implements PanelStatisticsPort {

    private final PanelDomainService panelDomainService;
    private final SanatoriumQueryService sanatoriumQueryService;
    private final SanatoriumScopeResolver scopeResolver;

    public PanelStatisticsPortAdapter(PanelDomainService panelDomainService,
                                      SanatoriumQueryService sanatoriumQueryService,
                                      SanatoriumScopeResolver scopeResolver) {
        this.panelDomainService = panelDomainService;
        this.sanatoriumQueryService = sanatoriumQueryService;
        this.scopeResolver = scopeResolver;
    }

    /**
     * 方法：getSystemOverviewStats
     *
     * @author zhanghongyu
     */
    @Override
    public SystemOverviewStats getSystemOverviewStats() {
        PanelNavInfo navInfo = panelDomainService.getNavDistribution();
        boolean govAdmin = scopeResolver.isGovAdmin();

        int bedCount;
        int bedInUse;
        if (govAdmin) {
            bedCount = defaultInt(sanatoriumQueryService.countBed());
            bedInUse = defaultInt(sanatoriumQueryService.countBedInUse());
        } else {
            List<Sanatorium> sanatoriums = scopeResolver.resolveScopedSanatoriums();
            bedCount = sanatoriums.stream().mapToInt(item -> defaultInt(item.getBedCount())).sum();
            bedInUse = sanatoriums.stream().mapToInt(item -> defaultInt(item.getBedInUse())).sum();
        }

        return SystemOverviewStats.builder()
                .scope(govAdmin ? "global" : "organization")
                .scopeName(govAdmin ? "全局" : scopeResolver.resolveScopeName())
                .sanaCount(defaultInt(navInfo.getSanaCount()))
                .elderCount(defaultInt(navInfo.getElderCount()))
                .nurseCount(defaultInt(navInfo.getNurseCount()))
                .medicineCount(defaultInt(navInfo.getMedicineCount()))
                .affiliationCount(defaultInt(navInfo.getAffiliationCount()))
                .bedCount(bedCount)
                .bedInUse(bedInUse)
                .bedUseRate(defaultDouble(navInfo.getUseRate()))
                .bedUseRatePercent(formatPercent(defaultDouble(navInfo.getUseRate())))
                .build();
    }

    /**
     * 方法：getRegionSanatoriumDistribution
     *
     * @author zhanghongyu
     */
    @Override
    public List<RegionSanatoriumCount> getRegionSanatoriumDistribution() {
        return panelDomainService.regionSanaCount().stream()
                .map(item -> RegionSanatoriumCount.builder()
                        .regionName(item.getRegionName())
                        .count(item.getSanaCount() == null ? 0 : item.getSanaCount().intValue())
                        .build())
                .toList();
    }

    /**
     * 方法：defaultInt
     *
     * @author zhanghongyu
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 方法：defaultDouble
     *
     * @author zhanghongyu
     */
    private double defaultDouble(Double value) {
        return value == null ? 0D : value;
    }

    /**
     * 方法：formatPercent
     *
     * @author zhanghongyu
     */
    private String formatPercent(double ratio) {
        return String.format("%.2f%%", ratio * 100);
    }
}