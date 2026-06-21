package com.zhiling.framework.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 区域机构数量分布稳定模型。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionSanatoriumCount {

    private String regionName;
    private Integer count;
}