package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

 /**
 * 用户实体类
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("sys_user")
public class User extends BaseEntity {
    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 手机号
     */
    @TableField("phone_number")
    private String phoneNumber;

    /**
     * 头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 所属机构ID（行级权限）
     */
    @TableField("sana_id")
    private Long sanaId;

    /**
     * 多机构授权范围（非表字段，仅用于接口返回）
     */
    @TableField(exist = false)
    private Set<Long> sanaScopeIds;
}
