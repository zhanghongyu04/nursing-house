package com.zhiling.framework.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 机构详情统计稳定模型。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanatoriumDetailStats {

    private Long id;
    private String sanaName;
    private String regionName;
    private String address;
    private Integer status;
    private Integer elderCount;
    private Integer nurseCount;
    private Integer medicineCount;
    private Integer bedCount;
    private Integer bedInUse;
    private Double bedUseRate;
    private String bedUseRatePercent;
    private String uscc;
    private String legalPersons;
    private String legalPhone;
}