package com.zhiling.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiling.model.entity.Role;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper
 *
 * @author zhanghongyu
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据用户ID获取角色列表（复杂查询，保留注解）
     */
    @Select("select distinct sr.* from sys_role sr " +
            "left join sys_user_role sur on sr.id=sur.role_id " +
            "where sur.user_id=#{userId} and sur.status=0 and sr.status=0")
    List<Role> getRoleListByUserId(Long userId);

    /**
     * 插入用户角色关系
     */
    @Insert("insert into sys_user_role(role_id,user_id) values(#{roleId},#{userId})")
    Boolean insertRoleUser(@Param("roleId") Long roleId, @Param("userId") Long userId);

    /**
     * 删除用户角色关系
     */
    @Delete("delete from sys_user_role where user_id=#{userId}")
    Boolean deleteRoleUser(Long userId);

    /**
     * 根据角色标识查询角色ID
     */
    default Long selectIdByLabel(String label) {
        java.util.List<Role> roleList = selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Role>()
                        .eq(Role::getLabel, label)
                        .eq(Role::getStatus, 0)
                        .select(Role::getId)
        );
        if (roleList == null || roleList.isEmpty()) {
            return null;
        }
        return roleList.get(0).getId();
    }
}

