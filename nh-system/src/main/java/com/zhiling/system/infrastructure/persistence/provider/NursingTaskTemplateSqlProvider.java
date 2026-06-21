package com.zhiling.system.infrastructure.persistence.provider;

/**

 * NursingTaskTemplateSqlProvider

 *

 * @author zhanghongyu

 */

public class NursingTaskTemplateSqlProvider {

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    public String page() {
        return """
                <script>
                SELECT t.*, s.sana_name AS sana_name, e.elder_name AS elder_name, u.username AS assignee_username
                FROM tb_nursing_task_template t
                LEFT JOIN tb_sanatorium s ON t.sana_id = s.id
                LEFT JOIN tb_elder e ON t.elder_id = e.id
                LEFT JOIN sys_user u ON t.assignee_user_id = u.id
                WHERE 1=1
                <if test='dto.sanaScopeIds != null and dto.sanaScopeIds.size() > 0'>
                  AND t.sana_id IN
                  <foreach collection='dto.sanaScopeIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>
                </if>
                <if test='dto.taskTitle != null and dto.taskTitle != ""'>AND t.task_title LIKE CONCAT('%', #{dto.taskTitle}, '%')</if>
                <if test='dto.enabled != null'>AND t.enabled = #{dto.enabled}</if>
                ORDER BY t.create_time DESC
                </script>
                """;
    }
}