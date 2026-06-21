package com.zhiling.system.infrastructure.persistence.provider;

/**

 * UserSqlProvider

 *

 * @author zhanghongyu

 */

public class UserSqlProvider {

    /**
     * 方法：pageUser
     *
     * @author zhanghongyu
     */
    public String pageUser() {
        return """
                <script>
                SELECT DISTINCT u.*
                FROM sys_user u
                LEFT JOIN sys_user_role ur ON u.id = ur.user_id
                WHERE 1=1
                <if test='dto.username != null and dto.username != ""'>AND u.username LIKE CONCAT('%', #{dto.username}, '%')</if>
                <if test='dto.sanaId != null'>AND u.sana_id = #{dto.sanaId}</if>
                <if test='dto.sanaScopeIds != null and dto.sanaScopeIds.size() > 0'>
                  AND u.sana_id IN
                  <foreach collection='dto.sanaScopeIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>
                </if>
                <if test='dto.roleId != null'>AND ur.role_id = #{dto.roleId}</if>
                ORDER BY u.create_time DESC
                </script>
                """;
    }
}
