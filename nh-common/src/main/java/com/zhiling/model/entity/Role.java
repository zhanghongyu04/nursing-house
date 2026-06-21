package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

 /**
 * 角色实体类
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("sys_role")
public class Role extends BaseEntity {
    /**
     * 角色名称
     */
    @TableField("role_name")
    private String roleName;

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
}
