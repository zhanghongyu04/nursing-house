package com.zhiling.system.infrastructure.persistence.query.impl;

import com.zhiling.model.entity.User;
import com.zhiling.model.vo.UserNavVo;
import com.zhiling.system.infrastructure.persistence.mapper.UserMapper;
import com.zhiling.system.infrastructure.persistence.query.UserQueryService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户查询服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class UserQueryServiceImpl implements UserQueryService {

    private final UserMapper userMapper;

    /**
     * 构造器：UserQueryServiceImpl
     *
     * @author zhanghongyu
     */
    public UserQueryServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 方法：selectByUsername
     *
     * @author zhanghongyu
     */
    @Override
    public User selectByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return userMapper.findUserVoForLogin(username);
    }

    /**
     * 方法：selectById
     *
     * @author zhanghongyu
     */
    @Override
    public User selectById(Long id) {
        if (id == null) {
            return null;
        }
        return userMapper.selectById(id);
    }

    /**
     * 方法：listSanaScopeIdsByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Long> listSanaScopeIdsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Long> scopeIds = userMapper.listSanaScopeIdsByUserId(userId);
        return scopeIds != null ? scopeIds : Collections.emptyList();
    }

    /**
     * 方法：listSanaScopeIdsByUserIds
     *
     * @author zhanghongyu
     */
    @Override
    public Map<Long, List<Long>> listSanaScopeIdsByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = userMapper.listSanaScopeIdsByUserIds(userIds);
        Map<Long, List<Long>> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long userId = ((Number) row.get("user_id")).longValue();
            Long sanaId = ((Number) row.get("sana_id")).longValue();
            result.computeIfAbsent(userId, k -> new java.util.ArrayList<>()).add(sanaId);
        }
        return result;
    }

    /**
     * 方法：existsById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean existsById(Long userId) {
        if (userId == null) {
            return false;
        }
        return userMapper.selectById(userId) != null;
    }

    /**
     * 方法：selectIdByUsername
     *
     * @author zhanghongyu
     */
    @Override
    public Long selectIdByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return userMapper.selectIdByUsername(username);
    }

    /**
     * 方法：getUserNavInfo
     *
     * @author zhanghongyu
     */
    @Override
    public UserNavVo getUserNavInfo(Long userId) {
        if (userId == null) {
            return null;
        }
        return userMapper.getUserNavInfo(userId);
    }

    /**
     * 方法：getPassword
     *
     * @author zhanghongyu
     */
    @Override
    public String getPassword(Long userId) {
        if (userId == null) {
            return null;
        }
        return userMapper.getPassword(userId);
    }
}