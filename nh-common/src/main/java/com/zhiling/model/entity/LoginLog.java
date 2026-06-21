package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户登录日志。
 *
 * @author zhanghongyu
 */
@Data
@TableName("sys_login_log")
public class LoginLog extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("login_ip")
    private String loginIp;

    @TableField("login_location")
    private String loginLocation;

    @TableField("user_agent")
    private String userAgent;

    @TableField("success_flag")
    private Integer successFlag;

    @TableField("message")
    private String message;

    @TableField("login_time")
    private LocalDateTime loginTime;
}
