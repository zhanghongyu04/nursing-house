package com.zhiling.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限管理资源树节点。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResourceTreeVo {
    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 资源编号。
     */
    private String resourceNo;

    /**
     * 父资源编号。
     */
    private String parentResourceNo;

    /**
     * 资源名称。
     */
    private String resourceName;

    /**
     * 资源类型。
     */
    private String resourceType;

    /**
     * 请求地址。
     */
    private String requestPath;

    /**
     * 权限标识。
     */
    private String label;

    /**
     * 排序号。
     */
    private Integer sortNo;

    /**
     * 图标。
     */
    private String icon;

    /**
     * 子资源。
     */
    @Builder.Default
    private List<PermissionResourceTreeVo> children = new ArrayList<>();
}
