package com.zhiling.system.infrastructure.persistence.provider;

/**

 * ElderSqlProvider

 *

 * @author zhanghongyu

 */

public class ElderSqlProvider {

    /**
     * 方法：update
     *
     * @author zhanghongyu
     */
    public String update() {
        return """
                <script>
                update tb_elder
                <set>
                  <if test='elder.sanaId != null'>sana_id = #{elder.sanaId},</if>
                  <if test='elder.elderName != null and elder.elderName != ""'>elder_name = #{elder.elderName},</if>
                  <if test='elder.sex != null'>sex = #{elder.sex},</if>
                  <if test='elder.age != null'>age = #{elder.age},</if>
                  <if test='elder.idNumber != null and elder.idNumber != ""'>id_number = #{elder.idNumber},</if>
                  <if test='elder.phoneNumber != null'>phone_number = #{elder.phoneNumber},</if>
                  <if test='elder.homeAddress != null and elder.homeAddress != ""'>home_address = #{elder.homeAddress},</if>
                  <if test='elder.familySituation != null'>family_situation = #{elder.familySituation},</if>
                  <if test='elder.occupiedBedType != null'>occupied_bed_type = #{elder.occupiedBedType},</if>
                  <if test='elder.roomNumber != null'>room_number = #{elder.roomNumber},</if>
                  <if test='elder.guardianName != null and elder.guardianName != ""'>guardian_name = #{elder.guardianName},</if>
                  <if test='elder.guardianPhone != null and elder.guardianPhone != ""'>guardian_phone = #{elder.guardianPhone},</if>
                  <if test='elder.selfCare != null'>self_care = #{elder.selfCare},</if>
                  <if test='elder.inpatientsTime != null'>inpatients_time = #{elder.inpatientsTime},</if>
                  <if test='elder.outpatientsTime != null'>outpatients_time = #{elder.outpatientsTime},</if>
                  <if test='elder.fees != null'>fees = #{elder.fees},</if>
                </set>
                where id = #{elder.id}
                </script>
                """;
    }

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    public String page() {
        return """
                <script>
                SELECT
                  e.id,
                  e.sana_id,
                  s.sana_name,
                  e.elder_name,
                  e.sex,
                  e.age,
                  e.id_number,
                  e.phone_number,
                  e.home_address,
                  e.family_situation,
                  e.occupied_bed_type,
                  e.room_number,
                  e.guardian_name,
                  e.guardian_phone,
                  e.self_care,
                  e.inpatients_time,
                  e.outpatients_time,
                  e.fees,
                  e.status,
                  e.create_time,
                  e.update_time
                FROM tb_elder e
                INNER JOIN tb_sanatorium s ON e.sana_id = s.id
                <where>
                  <if test='dto.elderName != null and dto.elderName != ""'>AND e.elder_name LIKE CONCAT('%', #{dto.elderName}, '%')</if>
                  <if test='dto.occupiedBedType != null'>
                    <choose>
                      <when test='dto.occupiedBedType == 0'>AND (e.occupied_bed_type = 0 OR e.occupied_bed_type IS NULL)</when>
                      <otherwise>AND e.occupied_bed_type = #{dto.occupiedBedType}</otherwise>
                    </choose>
                  </if>
                  <if test='dto.roomNumber != null and dto.roomNumber != ""'>AND e.room_number LIKE CONCAT('%', #{dto.roomNumber}, '%')</if>
                  <if test='dto.inpatientsTime != null and dto.inpatientsTime != ""'>AND e.inpatients_time = #{dto.inpatientsTime}</if>
                  <if test='dto.sanaName != null and dto.sanaName != ""'>AND s.sana_name LIKE CONCAT('%', #{dto.sanaName}, '%')</if>
                  <if test='dto.sanaId != null'>AND e.sana_id = #{dto.sanaId}</if>
                  <if test='dto.elderIds != null and dto.elderIds.size() > 0'>
                    AND e.id IN
                    <foreach collection='dto.elderIds' item='eid' open='(' separator=',' close=')'>#{eid}</foreach>
                  </if>
                  <if test='dto.sanaScopeIds != null and dto.sanaScopeIds.size() > 0'>
                    AND e.sana_id IN
                    <foreach collection='dto.sanaScopeIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>
                  </if>
                  <if test='dto.familySituation != null'>AND e.family_situation = #{dto.familySituation}</if>
                  <if test='dto.selfCare != null'>AND e.self_care = #{dto.selfCare}</if>
                </where>
                ORDER BY e.create_time DESC
                </script>
                """;
    }

    /**
     * 方法：pageSanaElderList
     *
     * @author zhanghongyu
     */
    public String pageSanaElderList() {
        return """
                <script>
                SELECT
                  e.id,
                  e.sana_id,
                  s.sana_name,
                  e.elder_name,
                  e.sex,
                  e.age,
                  e.id_number,
                  e.phone_number,
                  e.home_address,
                  e.family_situation,
                  e.occupied_bed_type,
                  e.room_number,
                  e.guardian_name,
                  e.guardian_phone,
                  e.self_care,
                  e.inpatients_time,
                  e.outpatients_time,
                  e.fees,
                  e.status,
                  e.create_time,
                  e.update_time
                FROM tb_elder e
                INNER JOIN tb_sanatorium s ON e.sana_id = s.id
                <where>
                  <if test='dto.sanaName != null and dto.sanaName != ""'>AND s.sana_name LIKE CONCAT('%', #{dto.sanaName}, '%')</if>
                  <if test='dto.sanaId != null'>AND e.sana_id = #{dto.sanaId}</if>
                  <if test='dto.sanaScopeIds != null and dto.sanaScopeIds.size() > 0'>
                    AND e.sana_id IN
                    <foreach collection='dto.sanaScopeIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>
                  </if>
                  <if test='dto.selfCare != null'>AND e.self_care = #{dto.selfCare}</if>
                </where>
                ORDER BY e.create_time DESC
                </script>
                """;
    }
}
