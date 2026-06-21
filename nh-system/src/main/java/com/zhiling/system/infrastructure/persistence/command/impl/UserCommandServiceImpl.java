package com.zhiling.system.infrastructure.persistence.command.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.UserPageQueryDto;
import com.zhiling.model.entity.User;
import com.zhiling.system.infrastructure.persistence.command.UserCommandService;
import com.zhiling.system.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户命令服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserMapper userMapper;

    /**
     * 构造器：UserCommandServiceImpl
     *
     * @author zhanghongyu
     */
    public UserCommandServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 方法：insert
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insert(User user) {
        return userMapper.insert(user) > 0;
    }

    /**
     * 方法：updateById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updateById(User user) {
        return userMapper.updateById(user) > 0;
    }

    /**
     * 方法：deleteById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean deleteById(Long userId) {
        return userMapper.deleteById(userId) > 0;
    }

    /**
     * 方法：updatePasswordByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updatePasswordByUserId(Long userId, String encodedPassword) {
        return userMapper.updatePasswordByUsername(userId, encodedPassword);
    }

    /**
     * 方法：deleteSanaScopesByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public boolean deleteSanaScopesByUserId(Long userId) {
        return userMapper.deleteSanaScopesByUserId(userId) >= 0;
    }

    /**
     * 方法：insertSanaScopes
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insertSanaScopes(Long userId, List<Long> sanaIds, Integer scopeType, String remark) {
        return userMapper.insertSanaScopes(userId, sanaIds, scopeType, remark) > 0;
    }

    /**
     * 方法：selectPage
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<User> selectPage(Page<User> page, UserPageQueryDto dto) {
        return userMapper.pageUser(page, dto);
    }
}