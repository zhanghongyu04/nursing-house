package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

 /**
 * 角色资源关联实体类
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("sys_role_resource")
public class RoleResource extends BaseEntity {
    /**
     * 角色ID
     */
    @TableField("role_id")
    private Long roleId;

    /**
     * 资源编号
     */
    @TableField("resource_no")
    private String resourceNo;
}