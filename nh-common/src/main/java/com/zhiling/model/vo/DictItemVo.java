package com.zhiling.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典项返回对象
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictItemVo {
    /**
     * 字典类型编码
     */
    private String dictTypeCode;

    /**
     * 字典值
     */
    private String itemValue;

    /**
     * 字典标签
     */
    private String itemLabel;

    /**
     * 字典说明
     */
    private String itemDesc;

    /**
     * 排序
     */
    private Integer sortNo;
}
