package com.zhiling.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 登录态用户信息。
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginVo {

    private Long id;
    private String username;
    private String password;
    private String email;
    private String phoneNumber;
    private String avatar;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    private String remark;
    private String dataState;
    private Long sanaId;

    private String token;
    private Set<String> roleLabels;
    private Set<String> resourcePaths;
    private Set<Long> sanaScopeIds;
}
