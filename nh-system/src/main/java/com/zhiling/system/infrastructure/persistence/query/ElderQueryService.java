package com.zhiling.system.infrastructure.persistence.query;

import com.zhiling.model.entity.Elder;
import com.zhiling.model.dto.ElderPageQueryDto;
import com.zhiling.model.dto.SanatoriumDetailPageQueryDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 老人查询服务接口。
 *
 * 封装 ElderMapper 的查询操作，供 adapter 层使用。
 *
 * @author zhanghongyu
 */
public interface ElderQueryService {

    /**
     * 查询老人总数
     */
    Integer count();

    /**
     * 按机构查询老人总数
     */
    Integer countBySanaId(Long sanaId);

    /**
     * 按机构范围查询老人总数
     */
    Integer countBySanaIds(Set<Long> sanaIds);

    /**
     * 查询老人自理能力分布（按机构名称）
     */
    List<Map<String, Object>> queryElderDistribution(String sanaName);

    /**
     * 查询全局老人自理能力分布
     */
    List<Map<String, Object>> queryAllElderDistribution();

    /**
     * 按机构 ID 查询老人自理能力分布
     */
    List<Map<String, Object>> queryElderDistributionBySanaId(Long sanaId);

    /**
     * 根据 ID 查询老人。
     */
    Elder selectById(Long id);

    /**
     * 分页查询老人。
     */
    com.baomidou.mybatisplus.core.metadata.IPage<Elder> selectPage(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Elder> page,
            ElderPageQueryDto dto);

    /**
     * 分页查询机构详情老人列表。
     */
    com.baomidou.mybatisplus.core.metadata.IPage<Elder> selectPageSanaElderList(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Elder> page,
            SanatoriumDetailPageQueryDto dto);

    /**
     * 查询全部老人。
     */
    List<Elder> listAll();
}