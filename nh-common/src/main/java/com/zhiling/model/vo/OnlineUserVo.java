package com.zhiling.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 在线用户监控视图。
 *
 * @author zhanghongyu
 */
@Data
public class OnlineUserVo {
    private Long userId;
    private String username;
    private Long sanaId;
    private Set<String> roleLabels;
    private String token;
    private Long expireSeconds;
    private LocalDateTime lastLoginTime;
}
