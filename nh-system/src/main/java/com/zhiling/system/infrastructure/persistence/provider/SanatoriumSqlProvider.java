package com.zhiling.system.infrastructure.persistence.provider;

/**

 * SanatoriumSqlProvider

 *

 * @author zhanghongyu

 */

public class SanatoriumSqlProvider {

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    public String page() {
        return """
                <script>
                SELECT * FROM tb_sanatorium
                <where>
                  <if test='dto.sanaName != null and dto.sanaName != ""'>AND sana_name LIKE CONCAT('%', #{dto.sanaName}, '%')</if>
                  <if test='dto.sanaAffiliation != null and dto.sanaAffiliation != ""'>AND sana_affiliation LIKE CONCAT('%', #{dto.sanaAffiliation}, '%')</if>
                  <if test='dto.sanaAddress != null and dto.sanaAddress != ""'>AND sana_address LIKE CONCAT('%', #{dto.sanaAddress}, '%')</if>
                  <if test='dto.status != null'>AND status = #{dto.status}</if>
                  <if test='dto.sanaId != null'>AND id = #{dto.sanaId}</if>
                  <if test='dto.sanatoriumIds != null and dto.sanatoriumIds.size() > 0'>
                    AND id IN
                    <foreach collection='dto.sanatoriumIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>
                  </if>
                  <if test='dto.sanaScopeIds != null and dto.sanaScopeIds.size() > 0'>
                    AND id IN
                    <foreach collection='dto.sanaScopeIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>
                  </if>
                </where>
                ORDER BY create_time DESC
                </script>
                """;
    }
}
