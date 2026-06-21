package com.zhiling.system.infrastructure.persistence.provider;

/**
 * 护理日志动态SQL
 *
 * @author zhanghongyu
 */
public class NursingLogSqlProvider {

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    public String page() {
        return """
                <script>
                SELECT
                  l.id,
                  l.sana_id,
                  l.task_id,
                  l.elder_id,
                  l.nurse_user_id,
                  l.log_time,
                  l.content,
                  l.abnormal_flag,
                  l.attachment_urls,
                  l.create_time,
                  l.update_time,
                  s.sana_name,
                  e.elder_name,
                  u.username AS nurse_username,
                  t.task_title
                FROM tb_nursing_log l
                LEFT JOIN tb_sanatorium s ON l.sana_id = s.id
                LEFT JOIN tb_elder e ON l.elder_id = e.id
                LEFT JOIN sys_user u ON l.nurse_user_id = u.id
                LEFT JOIN tb_nursing_task t ON l.task_id = t.id
                <where>
                  <if test='dto.sanaId != null'>AND l.sana_id = #{dto.sanaId}</if>
                  <if test='dto.sanaScopeIds != null and dto.sanaScopeIds.size() > 0'>
                    AND l.sana_id IN
                    <foreach collection='dto.sanaScopeIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>
                  </if>
                  <if test='dto.taskId != null'>AND l.task_id = #{dto.taskId}</if>
                  <if test='dto.elderId != null'>AND l.elder_id = #{dto.elderId}</if>
                  <if test='dto.nurseUserId != null'>AND l.nurse_user_id = #{dto.nurseUserId}</if>
                  <if test='dto.logIds != null and dto.logIds.size() > 0'>
                    AND l.id IN
                    <foreach collection='dto.logIds' item='logId' open='(' separator=',' close=')'>#{logId}</foreach>
                  </if>
                  <if test='dto.abnormalFlag != null'>AND l.abnormal_flag = #{dto.abnormalFlag}</if>
                  <if test='dto.content != null and dto.content != \"\"'>AND l.content LIKE CONCAT('%', #{dto.content}, '%')</if>
                  <if test='dto.logTimeBegin != null'>AND l.log_time <![CDATA[>=]]> #{dto.logTimeBegin}</if>
                  <if test='dto.logTimeEnd != null'>AND l.log_time <![CDATA[<=]]> #{dto.logTimeEnd}</if>
                </where>
                ORDER BY l.log_time DESC, l.create_time DESC
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
                  l.id,
                  l.sana_id,
                  l.task_id,
                  l.elder_id,
                  l.nurse_user_id,
                  l.log_time,
                  l.content,
                  l.abnormal_flag,
                  l.attachment_urls,
                  l.create_time,
                  l.update_time,
                  s.sana_name,
                  e.elder_name,
                  u.username AS nurse_username,
                  t.task_title
                FROM tb_nursing_log l
                LEFT JOIN tb_sanatorium s ON l.sana_id = s.id
                LEFT JOIN tb_elder e ON l.elder_id = e.id
                LEFT JOIN sys_user u ON l.nurse_user_id = u.id
                LEFT JOIN tb_nursing_task t ON l.task_id = t.id
                <where>
                  AND l.nurse_user_id = #{dto.nurseUserId}
                  <if test='dto.sanaId != null'>AND l.sana_id = #{dto.sanaId}</if>
                  <if test='dto.sanaScopeIds != null and dto.sanaScopeIds.size() > 0'>
                    AND l.sana_id IN
                    <foreach collection='dto.sanaScopeIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>
                  </if>
                  <if test='dto.taskId != null'>AND l.task_id = #{dto.taskId}</if>
                  <if test='dto.elderId != null'>AND l.elder_id = #{dto.elderId}</if>
                  <if test='dto.abnormalFlag != null'>AND l.abnormal_flag = #{dto.abnormalFlag}</if>
                  <if test='dto.content != null and dto.content != \"\"'>AND l.content LIKE CONCAT('%', #{dto.content}, '%')</if>
                  <if test='dto.logTimeBegin != null'>AND l.log_time <![CDATA[>=]]> #{dto.logTimeBegin}</if>
                  <if test='dto.logTimeEnd != null'>AND l.log_time <![CDATA[<=]]> #{dto.logTimeEnd}</if>
                </where>
                ORDER BY l.log_time DESC, l.create_time DESC
                </script>
                """;
    }
}