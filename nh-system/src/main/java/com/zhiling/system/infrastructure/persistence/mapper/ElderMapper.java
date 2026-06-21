package com.zhiling.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.ElderDto;
import com.zhiling.model.dto.ElderPageQueryDto;
import com.zhiling.model.dto.SanatoriumDetailPageQueryDto;
import com.zhiling.model.entity.Elder;
import com.zhiling.system.infrastructure.persistence.provider.ElderSqlProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 老人信息 Mapper
 *
 * @author zhanghongyu
 */
@Mapper
public interface ElderMapper extends BaseMapper<Elder> {

    /**
     * 添加老人信息（使用 MyBatis-Plus）
     */
    default Boolean add(Elder elder) {
        return insert(elder) > 0;
    }

    /**
     * 删除老人信息（使用 MyBatis-Plus）
     */
    default Boolean removeById(Long id) {
        return deleteById(id) > 0;
    }

    /**
     * 修改老人信息
     */
    @UpdateProvider(type = ElderSqlProvider.class, method = "update")
    Boolean update(@Param("elder") ElderDto elder);

    /**
     * 分页查询老人信息
     */
    @SelectProvider(type = ElderSqlProvider.class, method = "page")
    IPage<Elder> page(Page<Elder> page, @Param("dto") ElderPageQueryDto dto);

    /**
     * 查询所有老人信息（使用 MyBatis-Plus）
     */
    default List<Elder> list() {
        return selectList(null);
    }

    /**
     * 不同自理能力老人数量分布（复杂 SQL，保留）
     */
    @Select("WITH all_self_care AS (" +
            "    SELECT 0 AS self_care UNION ALL " +
            "    SELECT 1 AS self_care UNION ALL " +
            "    SELECT 2 AS self_care UNION ALL " +
            "    SELECT 3 AS self_care UNION ALL " +
            "    SELECT 4 AS self_care " +
            ") " +
            "SELECT " +
            "    a.self_care, " +
            "    COALESCE(COUNT(e.id), 0) AS count " +
            "FROM all_self_care a " +
            "LEFT JOIN tb_elder e ON a.self_care = e.self_care " +
            "INNER JOIN tb_sanatorium s ON e.sana_id = s.id " +
            "WHERE s.sana_name = #{sanaName} " +
            "GROUP BY a.self_care " +
            "ORDER BY a.self_care")
    List<Map<String, Object>> queryElderDistribution(@Param("sanaName") String sanaName);

    /**
     * 查询全局老人自理能力分布
     */
    @Select("WITH all_self_care AS (" +
            "    SELECT 0 AS self_care UNION ALL " +
            "    SELECT 1 AS self_care UNION ALL " +
            "    SELECT 2 AS self_care UNION ALL " +
            "    SELECT 3 AS self_care UNION ALL " +
            "    SELECT 4 AS self_care " +
            ") " +
            "SELECT " +
            "    a.self_care, " +
            "    COALESCE(COUNT(e.id), 0) AS count " +
            "FROM all_self_care a " +
            "LEFT JOIN tb_elder e ON a.self_care = e.self_care " +
            "GROUP BY a.self_care " +
            "ORDER BY a.self_care")
    List<Map<String, Object>> queryAllElderDistribution();

    /**
     * 按机构查询老人自理能力分布
     */
    @Select("WITH all_self_care AS (" +
            "    SELECT 0 AS self_care UNION ALL " +
            "    SELECT 1 AS self_care UNION ALL " +
            "    SELECT 2 AS self_care UNION ALL " +
            "    SELECT 3 AS self_care UNION ALL " +
            "    SELECT 4 AS self_care " +
            ") " +
            "SELECT " +
            "    a.self_care, " +
            "    COALESCE(COUNT(e.id), 0) AS count " +
            "FROM all_self_care a " +
            "LEFT JOIN tb_elder e ON a.self_care = e.self_care AND e.sana_id = #{sanaId} " +
            "GROUP BY a.self_care " +
            "ORDER BY a.self_care")
    List<Map<String, Object>> queryElderDistributionBySanaId(@Param("sanaId") Long sanaId);

    /**
     * 分页查询养老院详情中的老人列表
     */
    @SelectProvider(type = ElderSqlProvider.class, method = "pageSanaElderList")
    IPage<Elder> pageSanaElderList(Page<Elder> page, @Param("dto") SanatoriumDetailPageQueryDto dto);

    /**
     * 查询老人总数
     */
    @Select("select count(*) from tb_elder")
    Integer selectCount();

    /**
     * 按机构查询老人总数
     */
    @Select("select count(*) from tb_elder where sana_id = #{sanaId}")
    Integer selectCountBySanaId(@Param("sanaId") Long sanaId);

    /**
     * 按机构范围查询老人总数
     */
    @Select({
            "<script>",
            "select count(*) from tb_elder",
            "where sana_id in",
            "<foreach collection='sanaIds' item='sid' open='(' separator=',' close=')'>",
            "#{sid}",
            "</foreach>",
            "</script>"
    })
    Integer selectCountBySanaIds(@Param("sanaIds") Set<Long> sanaIds);
}

