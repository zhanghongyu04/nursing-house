package com.zhiling.system.infrastructure.persistence.query;

import com.zhiling.model.entity.User;
import com.zhiling.model.vo.UserNavVo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户查询服务接口。
 *
 * 封装 UserMapper 的查询操作，供 adapter 层使用。
 *
 * @author zhanghongyu
 */
public interface UserQueryService {

    /**
     * 根据用户名查询用户
     */
    User selectByUsername(String username);

    /**
     * 根据 ID 查询用户
     */
    User selectById(Long id);

    /**
     * 查询用户的机构范围 IDs
     */
    List<Long> listSanaScopeIdsByUserId(Long userId);

    /**
     * 批量查询用户的机构范围 IDs（避免 N+1 查询）
     */
    Map<Long, List<Long>> listSanaScopeIdsByUserIds(List<Long> userIds);

    /**
     * 检查用户是否存在
     */
    boolean existsById(Long userId);

    /**
     * 根据用户名查询用户 ID
     */
    Long selectIdByUsername(String username);

    /**
     * 查询用户导航信息。
     */
    UserNavVo getUserNavInfo(Long userId);

    /**
     * 查询用户密码摘要。
     */
    String getPassword(Long userId);
}