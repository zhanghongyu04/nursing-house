package com.zhiling.framework.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统概览统计稳定模型。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemOverviewStats {

    private String scope;
    private String scopeName;
    private Integer sanaCount;
    private Integer elderCount;
    private Integer nurseCount;
    private Integer medicineCount;
    private Integer affiliationCount;
    private Integer bedCount;
    private Integer bedInUse;
    private Double bedUseRate;
    private String bedUseRatePercent;
}