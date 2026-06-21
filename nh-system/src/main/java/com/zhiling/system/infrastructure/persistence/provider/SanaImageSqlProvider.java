package com.zhiling.system.infrastructure.persistence.provider;

/**

 * SanaImageSqlProvider

 *

 * @author zhanghongyu

 */

public class SanaImageSqlProvider {

    /**
     * 方法：selectPage
     *
     * @author zhanghongyu
     */
    public String selectPage() {
        return """
                <script>
                select * from tb_sana_image
                <where>
                  <if test='sanaImage.sanaId != null'>and sana_id = #{sanaImage.sanaId}</if>
                  <if test='sanaImage.sanaScopeIds != null and sanaImage.sanaScopeIds.size() > 0'>
                    and sana_id in
                    <foreach collection='sanaImage.sanaScopeIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>
                  </if>
                </where>
                order by create_time desc
                </script>
                """;
    }
}
