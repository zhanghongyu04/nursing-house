package com.zhiling.common.security;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 认证用户主体。
 *
 * @author zhanghongyu
 */
public class UserAuth implements UserDetails {

    private String id;
    private String username;
    private String password;
    private Collection<SimpleGrantedAuthority> authorities;
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

    /**
     * 构造器：UserAuth
     *
     * @author zhanghongyu
     */
    public UserAuth() {
    }

    /**
     * 构造器：UserAuth
     *
     * @author zhanghongyu
     */
    public UserAuth(LoginVo userVo) {
        this.setId(userVo.getId().toString());
        this.setUsername(userVo.getUsername());
        this.setPassword(userVo.getPassword());
        this.setAvatar(userVo.getAvatar());
        if (!ObjectUtil.isEmpty(userVo.getResourcePaths())) {
            authorities = new ArrayList<>();
            userVo.getResourcePaths().forEach(resourceRequestPath -> authorities.add(new SimpleGrantedAuthority(resourceRequestPath)));
        }
        this.setEmail(userVo.getEmail());
        this.setPhoneNumber(userVo.getPhoneNumber());
        this.setCreateTime(userVo.getCreateTime());
        this.setCreateBy(userVo.getCreateBy());
        this.setUpdateTime(userVo.getUpdateTime());
        this.setUpdateBy(userVo.getUpdateBy());
        this.setRemark(userVo.getRemark());
        this.setSanaId(userVo.getSanaId());
    }

    /**
     * 方法：getId
     *
     * @author zhanghongyu
     */
    public String getId() {
        return id;
    }

    /**
     * 方法：setId
     *
     * @author zhanghongyu
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 方法：getUsername
     *
     * @author zhanghongyu
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 方法：setUsername
     *
     * @author zhanghongyu
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 方法：getPassword
     *
     * @author zhanghongyu
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * 方法：setPassword
     *
     * @author zhanghongyu
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 方法：getAvatar
     *
     * @author zhanghongyu
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * 方法：setAvatar
     *
     * @author zhanghongyu
     */
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    /**
     * 方法：getEmail
     *
     * @author zhanghongyu
     */
    public String getEmail() {
        return email;
    }

    /**
     * 方法：setEmail
     *
     * @author zhanghongyu
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 方法：getPhoneNumber
     *
     * @author zhanghongyu
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * 方法：setPhoneNumber
     *
     * @author zhanghongyu
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * 方法：getCreateBy
     *
     * @author zhanghongyu
     */
    public Long getCreateBy() {
        return createBy;
    }

    /**
     * 方法：setCreateBy
     *
     * @author zhanghongyu
     */
    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    /**
     * 方法：getCreateTime
     *
     * @author zhanghongyu
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 方法：setCreateTime
     *
     * @author zhanghongyu
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 方法：getUpdateBy
     *
     * @author zhanghongyu
     */
    public Long getUpdateBy() {
        return updateBy;
    }

    /**
     * 方法：setUpdateBy
     *
     * @author zhanghongyu
     */
    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    /**
     * 方法：getUpdateTime
     *
     * @author zhanghongyu
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 方法：setUpdateTime
     *
     * @author zhanghongyu
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 方法：getRemark
     *
     * @author zhanghongyu
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 方法：setRemark
     *
     * @author zhanghongyu
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 方法：getDataState
     *
     * @author zhanghongyu
     */
    public String getDataState() {
        return dataState;
    }

    /**
     * 方法：setDataState
     *
     * @author zhanghongyu
     */
    public void setDataState(String dataState) {
        this.dataState = dataState;
    }

    /**
     * 方法：getSanaId
     *
     * @author zhanghongyu
     */
    public Long getSanaId() {
        return sanaId;
    }

    /**
     * 方法：setSanaId
     *
     * @author zhanghongyu
     */
    public void setSanaId(Long sanaId) {
        this.sanaId = sanaId;
    }

    /**
     * 方法：getAuthorities
     *
     * @author zhanghongyu
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    /**
     * 方法：isAccountNonExpired
     *
     * @author zhanghongyu
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 方法：isAccountNonLocked
     *
     * @author zhanghongyu
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 方法：isCredentialsNonExpired
     *
     * @author zhanghongyu
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 方法：isEnabled
     *
     * @author zhanghongyu
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}