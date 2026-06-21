package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限资源实体类
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("sys_resource")
public class Resource extends BaseEntity {
    /**
     * 资源编号
     */
    @TableField("resource_no")
    private String resourceNo;

    /**
     * 父资源编号
     */
    @TableField("parent_resource_no")
    private String parentResourceNo;

    /**
     * 资源名称
     */
    @TableField("resource_name")
    private String resourceName;

    /**
     * 资源类型
     */
    @TableField("resource_type")
    private String resourceType;

    /**
     * 请求地址
     */
    @TableField("request_path")
    private String requestPath;

    /**
     * 权限标识
     */
    @TableField("label")
    private String label;

    /**
     * 排序
     */
    @TableField("sort_no")
    private Integer sortNo;

    /**
     * 图标
     */
    @TableField("icon")
    private String icon;
}