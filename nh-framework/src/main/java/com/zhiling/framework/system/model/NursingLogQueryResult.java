package com.zhiling.framework.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 护理日志列表查询结果。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NursingLogQueryResult {

    private String scope;
    private String scopeName;
    private Long total;
    private List<NursingLogRecord> records;
}
