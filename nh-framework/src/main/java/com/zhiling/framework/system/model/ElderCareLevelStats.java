package com.zhiling.framework.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 老人自理能力分布稳定模型。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElderCareLevelStats {

    private String scope;
    private String scopeName;
    private Map<String, Integer> distribution;
}