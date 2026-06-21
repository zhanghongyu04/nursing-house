package com.zhiling.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.UserPageQueryDto;
import com.zhiling.model.entity.User;
import com.zhiling.model.vo.UserNavVo;
import com.zhiling.system.infrastructure.persistence.provider.UserSqlProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.SelectProvider;
import java.util.List;
import java.util.Map;

/**
 * 用户 Mapper（继承 BaseMapper）
 *
 * @author zhanghongyu
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 登录查询（保留注解方式）
     */
    @Select("select * from sys_user where username=#{username} and status=0")
    User findUserVoForLogin(String username);

    /**
     * 获取用户中心信息（复杂查询，保留注解）
     */
    @Select("select u.username,u.avatar,u.phone_number,u.email,u.sana_id from sys_user u where u.id=#{id} and u.status=0")
    UserNavVo getUserNavInfo(Long id);

    /**
     * 查询用户机构授权范围（仅启用状态）
     */
    @Select("select sana_id from sys_user_sana_scope where user_id = #{userId} and status = 0")
    List<Long> listSanaScopeIdsByUserId(@Param("userId") Long userId);

    /**
     * 批量查询用户机构授权范围（避免 N+1 查询）
     */
    @Select({
            "<script>",
            "select user_id, sana_id from sys_user_sana_scope where user_id in ",
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            " and status = 0",
            "</script>"
    })
    List<Map<String, Object>> listSanaScopeIdsByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 覆盖用户机构授权范围（先删后插）
     */
    @Delete("delete from sys_user_sana_scope where user_id = #{userId}")
    int deleteSanaScopesByUserId(@Param("userId") Long userId);

    @Insert({
            "<script>",
            "insert into sys_user_sana_scope(user_id, sana_id, scope_type, status, remark) values",
            "<foreach collection='sanaIds' item='sid' separator=','>",
            "(#{userId}, #{sid}, #{scopeType}, 0, #{remark})",
            "</foreach>",
            "</script>"
    })
    int insertSanaScopes(@Param("userId") Long userId,
                         @Param("sanaIds") List<Long> sanaIds,
                         @Param("scopeType") Integer scopeType,
                         @Param("remark") String remark);

    /**
     * 分页查询用户列表（使用 MyBatis-Plus 分页）
     */
    @SelectProvider(type = UserSqlProvider.class, method = "pageUser")
    IPage<User> pageUser(Page<User> page, @Param("dto") UserPageQueryDto userDto);

    /**
     * 根据用户名查询ID（使用 MyBatis-Plus Lambda）
     */
    default Long selectIdByUsername(String username) {
        User user = selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .select(User::getId)
        );
        return user != null ? user.getId() : null;
    }

    /**
     * 根据ID查询密码
     */
    default String getPassword(Long userId) {
        User user = selectById(userId);
        return user != null ? user.getPassword() : null;
    }

    /**
     * 重置密码
     */
    default Boolean updatePasswordByUsername(Long userId, String encodedPassword) {
        User user = new User();
        user.setId(userId);
        user.setPassword(encodedPassword);
        return updateById(user) > 0;
    }

    /**
     * 更新用户信息
     */
    default Boolean updateUserInfo(User user) {
        return updateById(user) > 0;
    }
}

