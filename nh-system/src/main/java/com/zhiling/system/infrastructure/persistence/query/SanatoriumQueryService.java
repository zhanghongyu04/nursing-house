package com.zhiling.system.infrastructure.persistence.query;

import com.zhiling.model.entity.Sanatorium;
import com.zhiling.model.dto.SanatoriumPageQueryDto;

import java.util.List;
import java.util.Set;

/**
 * 机构查询服务接口。
 *
 * 封装 SanatoriumMapper 的查询操作，供 adapter 层使用。
 *
 * @author zhanghongyu
 */
public interface SanatoriumQueryService {

    /**
     * 查询所有养老院
     */
    List<Sanatorium> listAll();

    /**
     * 根据 ID 批量查询
     */
    List<Sanatorium> listByIds(Set<Long> ids);

    /**
     * 根据名称查询 ID
     */
    Long selectIdByName(String sanaName);

    /**
     * 根据 ID 查询名称
     */
    String getNameById(Long sanaId);

    /**
     * 根据 ID 查询机构
     */
    Sanatorium selectById(Long id);

    /**
     * 精确匹配名称查询
     */
    Sanatorium selectByExactName(String sanaName);

    /**
     * 判断指定 USCC 是否已存在
     */
    boolean existsByUscc(String uscc);

    /**
     * 模糊匹配名称查询
     */
    List<Sanatorium> searchByKeyword(String keyword, Integer limit, Set<Long> scopeIds);

    /**
     * 统计养老院数量
     */
    Integer count();

    /**
     * 统计护理员数量
     */
    Integer countNurse();

    /**
     * 统计医护人员数量
     */
    Integer countMedicine();

    /**
     * 统计隶属单位数量
     */
    Integer countAffiliation();

    /**
     * 统计床位使用数
     */
    Integer countBedInUse();

    /**
     * 统计总床位数
     */
    Integer countBed();

    /**
     * 查询各区县养老院数量分布
     */
    List<com.zhiling.model.vo.RegionSanaCountVo> listRegionSanaCount();

    /**
     * 按机构查询区域分布。
     */
    List<com.zhiling.model.vo.RegionSanaCountVo> listRegionSanaCountBySanaId(Long sanaId);

    /**
     * 分页查询机构。
     */
    com.baomidou.mybatisplus.core.metadata.IPage<Sanatorium> selectPage(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Sanatorium> page,
            SanatoriumPageQueryDto dto);
}