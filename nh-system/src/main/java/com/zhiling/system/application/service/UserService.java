package com.zhiling.system.application.service;

import com.zhiling.common.security.LoginVo;
import com.zhiling.model.vo.UserNavVo;

/**
 * 用户服务接口
 *
 * @author zhanghongyu
 */
public interface UserService {
    /***
     *  查询用户构建对象
     * @param username
     * @return
     * @return:
     */
    LoginVo findUserVoForLogin(String username);


    /**
     * 获取用户中心信息
     * @return
     */
    UserNavVo getUserNavInfo();


    /**
     * 重置密码
     * @param oldPassword
     * @param newPassword
     * @return
     */
    Boolean resetPassword(String oldPassword,String newPassword);


    /**
     * 更新用户信息
     * @param userNavVo
     * @return
     */
    Boolean updateUserInfo(UserNavVo userNavVo);
}



