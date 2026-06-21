package com.zhiling.framework.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 护理任务列表查询结果。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NursingTaskQueryResult {

    private String scope;
    private String scopeName;
    private Long total;
    private List<NursingTaskRecord> records;
}
