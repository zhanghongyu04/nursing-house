package com.zhiling.system.panel.service;

import com.zhiling.model.vo.PanelNavInfo;
import com.zhiling.model.vo.RegionSanaCountVo;

import java.util.List;

/**
 * 大屏域服务接口。
 *
 * @author zhanghongyu
 */
public interface PanelDomainService {

    PanelNavInfo getNavDistribution();

    List<RegionSanaCountVo> regionSanaCount();

    List<RegionSanaCountVo> exportRegionStats();
}