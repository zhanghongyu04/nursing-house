package com.zhiling.system.infrastructure.persistence.repository.impl;

import com.zhiling.model.entity.User;
import com.zhiling.model.vo.UserNavVo;
import com.zhiling.system.application.repository.UserRepository;
import com.zhiling.system.infrastructure.persistence.command.UserCommandService;
import com.zhiling.system.infrastructure.persistence.query.UserQueryService;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户仓储实现。
 *
 * 委托给 UserQueryService 和 UserCommandService，
 * 实现 application 层定义的 UserRepository 契约。
 *
 * @author zhanghongyu
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;

    public UserRepositoryImpl(UserQueryService userQueryService,
                              UserCommandService userCommandService) {
        this.userQueryService = userQueryService;
        this.userCommandService = userCommandService;
    }

    // === 查询操作 ===

    /**
     * 方法：selectByUsername
     *
     * @author zhanghongyu
     */
    @Override
    public User selectByUsername(String username) {
        return userQueryService.selectByUsername(username);
    }

    /**
     * 方法：selectById
     *
     * @author zhanghongyu
     */
    @Override
    public User selectById(Long id) {
        return userQueryService.selectById(id);
    }

    /**
     * 方法：listSanaScopeIdsByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Long> listSanaScopeIdsByUserId(Long userId) {
        return userQueryService.listSanaScopeIdsByUserId(userId);
    }

    /**
     * 方法：existsById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean existsById(Long userId) {
        return userQueryService.existsById(userId);
    }

    /**
     * 方法：selectIdByUsername
     *
     * @author zhanghongyu
     */
    @Override
    public Long selectIdByUsername(String username) {
        return userQueryService.selectIdByUsername(username);
    }

    /**
     * 方法：getUserNavInfo
     *
     * @author zhanghongyu
     */
    @Override
    public UserNavVo getUserNavInfo(Long userId) {
        return userQueryService.getUserNavInfo(userId);
    }

    /**
     * 方法：getPassword
     *
     * @author zhanghongyu
     */
    @Override
    public String getPassword(Long userId) {
        return userQueryService.getPassword(userId);
    }

    // === 命令操作 ===

    /**
     * 方法：insert
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insert(User user) {
        return userCommandService.insert(user);
    }

    /**
     * 方法：updateById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updateById(User user) {
        return userCommandService.updateById(user);
    }

    /**
     * 方法：deleteById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean deleteById(Long userId) {
        return userCommandService.deleteById(userId);
    }

    /**
     * 方法：updatePasswordByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updatePasswordByUserId(Long userId, String encodedPassword) {
        return userCommandService.updatePasswordByUserId(userId, encodedPassword);
    }

    /**
     * 方法：deleteSanaScopesByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public boolean deleteSanaScopesByUserId(Long userId) {
        return userCommandService.deleteSanaScopesByUserId(userId);
    }

    /**
     * 方法：insertSanaScopes
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insertSanaScopes(Long userId, List<Long> sanaIds, Integer scopeType, String remark) {
        return userCommandService.insertSanaScopes(userId, sanaIds, scopeType, remark);
    }
}