package com.zhiling.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面板导航信息
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PanelNavInfo {
    private Integer sanaCount; //养老院数量
    private Integer elderCount; //老人数量
    private Double useRate; //床位使用率
    private Integer nurseCount; //护理人员数量
    private Integer medicineCount; //医护人员数量
    private Integer affiliationCount; //区域数量
}
