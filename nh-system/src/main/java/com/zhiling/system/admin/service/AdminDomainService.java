package com.zhiling.system.admin.service;

import com.zhiling.common.result.PageResult;
import com.zhiling.model.dto.UserDto;
import com.zhiling.model.dto.UserPageQueryDto;

/**
 * 管理员域服务接口。
 *
 * @author zhanghongyu
 */
public interface AdminDomainService {

    Boolean addUser(UserDto userDto);

    Boolean deleteUser(String username);

    PageResult pageUser(UserPageQueryDto userDto);

    Boolean updateUser(UserDto userDto);

    Boolean resetPassword(UserDto userDto);
}