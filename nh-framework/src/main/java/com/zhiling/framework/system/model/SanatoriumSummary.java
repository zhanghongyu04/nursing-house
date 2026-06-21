package com.zhiling.framework.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 机构摘要稳定模型。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanatoriumSummary {

    private Long id;
    private String sanaName;
    private String regionName;
    private String address;
    private Integer status;
    private Integer bedCount;
    private Integer bedInUse;
}