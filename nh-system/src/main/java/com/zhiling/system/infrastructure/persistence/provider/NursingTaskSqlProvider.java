package com.zhiling.system.infrastructure.persistence.provider;

/**
 * 护理任务动态SQL
 *
 * @author zhanghongyu
 */
public class NursingTaskSqlProvider {

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    public String page() {
        return """
                <script>
                SELECT
                  t.id,
                  t.sana_id,
                  t.elder_id,
                  t.task_title,
                  t.task_content,
                  t.task_type,
                  t.priority,
                  t.assignee_user_id,
                  t.assigner_user_id,
                  t.planned_start_time,
                  t.planned_end_time,
                  t.status,
                  t.completion_time,
                  t.remark,
                  t.create_time,
                  t.update_time,
                  s.sana_name,
                  e.elder_name,
                  ua.username AS assignee_username,
                  ub.username AS assigner_username
                FROM tb_nursing_task t
                LEFT JOIN tb_sanatorium s ON t.sana_id = s.id
                LEFT JOIN tb_elder e ON t.elder_id = e.id
                LEFT JOIN sys_user ua ON t.assignee_user_id = ua.id
                LEFT JOIN sys_user ub ON t.assigner_user_id = ub.id
                <where>
                  AND t.deleted = 0
                  <if test='dto.sanaId != null'>AND t.sana_id = #{dto.sanaId}</if>
                  <if test='dto.sanaScopeIds != null and dto.sanaScopeIds.size() > 0'>
                    AND t.sana_id IN
                    <foreach collection='dto.sanaScopeIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>
                  </if>
                  <if test='dto.elderId != null'>AND t.elder_id = #{dto.elderId}</if>
                  <if test='dto.assigneeUserId != null'>AND t.assignee_user_id = #{dto.assigneeUserId}</if>
                  <if test='dto.taskTitle != null and dto.taskTitle != \"\"'>AND t.task_title LIKE CONCAT('%', #{dto.taskTitle}, '%')</if>
                  <if test='dto.taskType != null'>AND t.task_type = #{dto.taskType}</if>
                  <if test='dto.priority != null'>AND t.priority = #{dto.priority}</if>
                  <if test='dto.status != null'>AND t.status = #{dto.status}</if>
                  <if test='dto.statuses != null and dto.statuses.size() > 0'>
                    AND t.status IN
                    <foreach collection='dto.statuses' item='taskStatus' open='(' separator=',' close=')'>#{taskStatus}</foreach>
                  </if>
                  <if test='dto.plannedStartBegin != null'>AND t.planned_start_time <![CDATA[>=]]> #{dto.plannedStartBegin}</if>
                  <if test='dto.plannedStartEnd != null'>AND t.planned_start_time <![CDATA[<=]]> #{dto.plannedStartEnd}</if>
                </where>
                ORDER BY t.create_time DESC
                </script>
                """;
    }

    /**
     * 方法：myPage
     *
     * @author zhanghongyu
     */
    public String myPage() {
        return """
                <script>
                SELECT
                  t.id,
                  t.sana_id,
                  t.elder_id,
                  t.task_title,
                  t.task_content,
                  t.task_type,
                  t.priority,
                  t.assignee_user_id,
                  t.assigner_user_id,
                  t.planned_start_time,
                  t.planned_end_time,
                  t.status,
                  t.completion_time,
                  t.remark,
                  t.create_time,
                  t.update_time,
                  s.sana_name,
                  e.elder_name,
                  ua.username AS assignee_username,
                  ub.username AS assigner_username
                FROM tb_nursing_task t
                LEFT JOIN tb_sanatorium s ON t.sana_id = s.id
                LEFT JOIN tb_elder e ON t.elder_id = e.id
                LEFT JOIN sys_user ua ON t.assignee_user_id = ua.id
                LEFT JOIN sys_user ub ON t.assigner_user_id = ub.id
                <where>
                  AND t.deleted = 0
                  AND t.assignee_user_id = #{dto.assigneeUserId}
                  <if test='dto.sanaId != null'>AND t.sana_id = #{dto.sanaId}</if>
                  <if test='dto.sanaScopeIds != null and dto.sanaScopeIds.size() > 0'>
                    AND t.sana_id IN
                    <foreach collection='dto.sanaScopeIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>
                  </if>
                  <if test='dto.elderId != null'>AND t.elder_id = #{dto.elderId}</if>
                  <if test='dto.taskTitle != null and dto.taskTitle != \"\"'>AND t.task_title LIKE CONCAT('%', #{dto.taskTitle}, '%')</if>
                  <if test='dto.taskType != null'>AND t.task_type = #{dto.taskType}</if>
                  <if test='dto.priority != null'>AND t.priority = #{dto.priority}</if>
                  <if test='dto.status != null'>AND t.status = #{dto.status}</if>
                  <if test='dto.statuses != null and dto.statuses.size() > 0'>
                    AND t.status IN
                    <foreach collection='dto.statuses' item='taskStatus' open='(' separator=',' close=')'>#{taskStatus}</foreach>
                  </if>
                  <if test='dto.plannedStartBegin != null'>AND t.planned_start_time <![CDATA[>=]]> #{dto.plannedStartBegin}</if>
                  <if test='dto.plannedStartEnd != null'>AND t.planned_start_time <![CDATA[<=]]> #{dto.plannedStartEnd}</if>
                </where>
                ORDER BY t.create_time DESC
                </script>
                """;
    }
}
