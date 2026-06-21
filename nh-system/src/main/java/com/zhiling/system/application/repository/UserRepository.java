package com.zhiling.system.application.repository;

import com.zhiling.model.entity.User;
import com.zhiling.model.vo.UserNavVo;

import java.util.List;
import java.util.Set;

/**
 * 用户仓储接口。
 *
 * 定义 application 层所需的用户数据访问契约，
 * 由 infrastructure 层提供实现，遵循依赖倒置原则。
 *
 * @author zhanghongyu
 */
public interface UserRepository {

    // === 查询操作 ===

    User selectByUsername(String username);

    User selectById(Long id);

    List<Long> listSanaScopeIdsByUserId(Long userId);

    boolean existsById(Long userId);

    Long selectIdByUsername(String username);

    UserNavVo getUserNavInfo(Long userId);

    String getPassword(Long userId);

    // === 命令操作 ===

    boolean insert(User user);

    boolean updateById(User user);

    boolean deleteById(Long userId);

    boolean updatePasswordByUserId(Long userId, String encodedPassword);

    boolean deleteSanaScopesByUserId(Long userId);

    boolean insertSanaScopes(Long userId, List<Long> sanaIds, Integer scopeType, String remark);
}