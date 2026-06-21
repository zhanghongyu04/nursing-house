package com.zhiling.system.panel.service;

import com.zhiling.model.entity.Sanatorium;
import com.zhiling.model.vo.RegionSanaCountVo;

import java.util.List;
import java.util.Set;

/**
 * 大屏统计查询服务。
 *
 * @author zhanghongyu
 */
public interface PanelStatisticsQueryService {

    Integer elderCount();

    Integer elderCountBySanaIds(Set<Long> sanaIds);

    Integer sanatoriumCount();

    Integer nurseCount();

    Integer medicineCount();

    Integer affiliationCount();

    Integer useBedCount();

    Integer bedCount();

    List<Sanatorium> listSanatoriumByIds(Set<Long> sanaIds);

    List<RegionSanaCountVo> regionSanaCount();
}
