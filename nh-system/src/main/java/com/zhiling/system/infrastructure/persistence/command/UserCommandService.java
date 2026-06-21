package com.zhiling.system.infrastructure.persistence.command;

import com.zhiling.model.dto.UserPageQueryDto;
import com.zhiling.model.entity.User;

import java.util.List;

/**
 * 用户命令服务接口。
 *
 * 封装 UserMapper 的写操作（insert/update/delete）。
 *
 * @author zhanghongyu
 */
public interface UserCommandService {

    /**
     * 添加用户
     */
    boolean insert(User user);

    /**
     * 更新用户
     */
    boolean updateById(User user);

    /**
     * 删除用户
     */
    boolean deleteById(Long userId);

    /**
     * 更新用户密码
     */
    boolean updatePasswordByUserId(Long userId, String encodedPassword);

    /**
     * 删除用户机构范围
     */
    boolean deleteSanaScopesByUserId(Long userId);

    /**
     * 插入用户机构范围
     */
    boolean insertSanaScopes(Long userId, List<Long> sanaIds, Integer scopeType, String remark);

    /**
     * 分页查询用户（返回 MyBatis-Plus IPage）
     */
    com.baomidou.mybatisplus.core.metadata.IPage<User> selectPage(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page,
            UserPageQueryDto dto);
}