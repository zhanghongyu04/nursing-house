package com.zhiling.framework.security.model;

import com.zhiling.common.security.LoginVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 当前请求的统一用户上下文。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserContext {

    private Long userId;
    private String username;
    private String email;
    private String phoneNumber;
    private String avatar;
    private String token;
    private String remark;
    private String dataState;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    private Long sanaId;
    private Set<Long> sanaScopeIds;
    private Set<String> roleLabels;
    private Set<String> resourcePaths;

    /**
     * 方法：toCurrentUser
     *
     * @author zhanghongyu
     */
    public CurrentUser toCurrentUser() {
        return CurrentUser.builder()
                .id(userId)
                .username(username)
                .email(email)
                .phoneNumber(phoneNumber)
                .avatar(avatar)
                .token(token)
                .remark(remark)
                .dataState(dataState)
                .createBy(createBy)
                .createTime(createTime)
                .updateBy(updateBy)
                .updateTime(updateTime)
                .accessScope(toAccessScope())
                .build();
    }

    /**
     * 方法：toAccessScope
     *
     * @author zhanghongyu
     */
    public AccessScope toAccessScope() {
        return AccessScope.builder()
                .userId(userId)
                .sanaId(sanaId)
                .sanaScopeIds(sanaScopeIds)
                .roleLabels(roleLabels)
                .resourcePaths(resourcePaths)
                .build();
    }

    /**
     * 方法：toLoginVo
     *
     * @author zhanghongyu
     */
    public LoginVo toLoginVo() {
        return LoginVo.builder()
                .id(userId)
                .username(username)
                .email(email)
                .phoneNumber(phoneNumber)
                .avatar(avatar)
                .token(token)
                .remark(remark)
                .dataState(dataState)
                .createBy(createBy)
                .createTime(createTime)
                .updateBy(updateBy)
                .updateTime(updateTime)
                .sanaId(sanaId)
                .sanaScopeIds(sanaScopeIds)
                .roleLabels(roleLabels)
                .resourcePaths(resourcePaths)
                .build();
    }

    /**
     * 方法：fromLoginVo
     *
     * @author zhanghongyu
     */
    public static CurrentUserContext fromLoginVo(LoginVo loginVo) {
        if (loginVo == null) {
            return null;
        }
        return CurrentUserContext.builder()
                .userId(loginVo.getId())
                .username(loginVo.getUsername())
                .email(loginVo.getEmail())
                .phoneNumber(loginVo.getPhoneNumber())
                .avatar(loginVo.getAvatar())
                .token(loginVo.getToken())
                .remark(loginVo.getRemark())
                .dataState(loginVo.getDataState())
                .createBy(loginVo.getCreateBy())
                .createTime(loginVo.getCreateTime())
                .updateBy(loginVo.getUpdateBy())
                .updateTime(loginVo.getUpdateTime())
                .sanaId(loginVo.getSanaId())
                .sanaScopeIds(loginVo.getSanaScopeIds())
                .roleLabels(loginVo.getRoleLabels())
                .resourcePaths(loginVo.getResourcePaths())
                .build();
    }
}