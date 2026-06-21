package com.zhiling.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
/**
 * RegionSanaCountVo
 *
 * @author zhanghongyu
 */
public class RegionSanaCountVo {
    private String regionName;
    private Long sanaCount;
}


