package com.zhiling.framework.system.port;

import com.zhiling.framework.system.model.RegionSanatoriumCount;
import com.zhiling.framework.system.model.SystemOverviewStats;

import java.util.List;

/**
 * 面板统计公开契约。
 *
 * @author zhanghongyu
 */
public interface PanelStatisticsPort {

    SystemOverviewStats getSystemOverviewStats();

    List<RegionSanatoriumCount> getRegionSanatoriumDistribution();
}