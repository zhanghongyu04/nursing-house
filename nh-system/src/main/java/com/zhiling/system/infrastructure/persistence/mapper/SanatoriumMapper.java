package com.zhiling.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.SanatoriumPageQueryDto;
import com.zhiling.model.entity.Sanatorium;
import com.zhiling.model.vo.RegionSanaCountVo;
import com.zhiling.system.infrastructure.persistence.provider.SanatoriumSqlProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.List;

/**
 * 养老院 Mapper
 *
 * @author zhanghongyu
 */
@Mapper
public interface SanatoriumMapper extends BaseMapper<Sanatorium> {

    /**
     * 查询所有养老院（使用 MyBatis-Plus）
     * 实际使用: selectList(null)
     */
    @Select("select * from tb_sanatorium")
    List<Sanatorium> list();

    /**
     * 分页查询（使用 MyBatis-Plus）
     */
    @SelectProvider(type = SanatoriumSqlProvider.class, method = "page")
    IPage<Sanatorium> page(Page<Sanatorium> page, @Param("dto") SanatoriumPageQueryDto dto);

    /**
     * 根据养老院名称查询ID
     */
    default Long selectIdByName(String sanaName) {
        Sanatorium sana = selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Sanatorium>()
                .eq(Sanatorium::getSanaName, sanaName)
                .select(Sanatorium::getId)
        );
        return sana != null ? sana.getId() : null;
    }

    /**
     * 统计查询方法（保留注解方式）
     */
    @Select("select count(*) from tb_sanatorium")
    Integer selectCount();

    @Select("select count(*) from tb_sanatorium where uscc = #{uscc}")
    Integer selectCountByUscc(@Param("uscc") String uscc);

    @Select("select sum(nursing_count) from tb_sanatorium")
    Integer selectNurseCount();

    @Select("select sum(medical_count) from tb_sanatorium")
    Integer selectMedicineCount();

    @Select("select count(distinct sana_affiliation) from tb_sanatorium")
    Integer selectAffiliationCount();

    @Select("select sum(bed_in_use) from tb_sanatorium")
    Integer selectUseBedCount();

    @Select("select sum(bed_count) from tb_sanatorium")
    Integer selectBedCount();

    @Select("select sana_name from tb_sanatorium where id = #{sanaId}")
    String getName(Long sanaId);

    @Select("SELECT d.region_name AS regionName, COALESCE(t.sana_count, 0) AS sanaCount " +
            "FROM ( " +
            "   SELECT '德城区' AS region_name UNION ALL " +
            "   SELECT '陵城区' UNION ALL " +
            "   SELECT '宁津县' UNION ALL " +
            "   SELECT '庆云县' UNION ALL " +
            "   SELECT '临邑县' UNION ALL " +
            "   SELECT '齐河县' UNION ALL " +
            "   SELECT '平原县' UNION ALL " +
            "   SELECT '夏津县' UNION ALL " +
            "   SELECT '武城县' UNION ALL " +
            "   SELECT '乐陵市' UNION ALL " +
            "   SELECT '禹城市' " +
            ") d " +
            "LEFT JOIN ( " +
            "   SELECT " +
            "       CASE " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%德城区%' THEN '德城区' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%陵城区%' THEN '陵城区' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%宁津%' THEN '宁津县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%庆云%' THEN '庆云县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%临邑%' THEN '临邑县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%齐河%' THEN '齐河县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%平原%' THEN '平原县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%夏津%' THEN '夏津县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%武城%' THEN '武城县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%乐陵%' THEN '乐陵市' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%禹城%' THEN '禹城市' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%开发区%' THEN '德城区' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%本级%' THEN '德城区' " +
            "           ELSE NULL " +
            "       END AS region_name, " +
            "       COUNT(*) AS sana_count " +
            "   FROM tb_sanatorium " +
            "   GROUP BY region_name " +
            ") t ON d.region_name = t.region_name")
    List<RegionSanaCountVo> listRegionSanaCount();

    @Select("SELECT d.region_name AS regionName, COALESCE(t.sana_count, 0) AS sanaCount " +
            "FROM ( " +
            "   SELECT '德城区' AS region_name UNION ALL " +
            "   SELECT '陵城区' UNION ALL " +
            "   SELECT '宁津县' UNION ALL " +
            "   SELECT '庆云县' UNION ALL " +
            "   SELECT '临邑县' UNION ALL " +
            "   SELECT '齐河县' UNION ALL " +
            "   SELECT '平原县' UNION ALL " +
            "   SELECT '夏津县' UNION ALL " +
            "   SELECT '武城县' UNION ALL " +
            "   SELECT '乐陵市' UNION ALL " +
            "   SELECT '禹城市' " +
            ") d " +
            "LEFT JOIN ( " +
            "   SELECT " +
            "       CASE " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%德城区%' THEN '德城区' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%陵城区%' THEN '陵城区' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%宁津%' THEN '宁津县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%庆云%' THEN '庆云县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%临邑%' THEN '临邑县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%齐河%' THEN '齐河县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%平原%' THEN '平原县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%夏津%' THEN '夏津县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%武城%' THEN '武城县' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%乐陵%' THEN '乐陵市' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%禹城%' THEN '禹城市' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%开发区%' THEN '德城区' " +
            "           WHEN CONCAT(IFNULL(sana_affiliation,''), IFNULL(sana_address,''), IFNULL(sana_name,'')) LIKE '%本级%' THEN '德城区' " +
            "           ELSE NULL " +
            "       END AS region_name, " +
            "       COUNT(*) AS sana_count " +
            "   FROM tb_sanatorium " +
            "   WHERE id = #{sanaId} " +
            "   GROUP BY region_name " +
            ") t ON d.region_name = t.region_name")
    List<RegionSanaCountVo> listRegionSanaCountBySanaId(@Param("sanaId") Long sanaId);
}

