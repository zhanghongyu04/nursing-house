package com.zhiling.system.application.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.ElderDto;
import com.zhiling.model.dto.ElderPageQueryDto;
import com.zhiling.model.dto.SanatoriumDetailPageQueryDto;
import com.zhiling.model.entity.Elder;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 老人仓储接口。
 *
 * 定义 application 层所需的老人数据访问契约，
 * 由 infrastructure 层提供实现，遵循依赖倒置原则。
 *
 * @author zhanghongyu
 */
public interface ElderRepository {

    // === 查询操作 ===

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
     * 根据 ID 查询老人
     */
    Elder selectById(Long id);

    /**
     * 分页查询老人
     */
    IPage<Elder> selectPage(Page<Elder> page, ElderPageQueryDto dto);

    /**
     * 分页查询机构详情老人列表
     */
    IPage<Elder> selectPageSanaElderList(Page<Elder> page, SanatoriumDetailPageQueryDto dto);

    /**
     * 查询全部老人
     */
    List<Elder> listAll();

    // === 命令操作 ===

    /**
     * 添加老人
     */
    boolean insert(Elder elder);

    /**
     * 删除老人
     */
    boolean removeById(Long id);

    /**
     * 更新老人
     */
    boolean update(ElderDto elderDto);
}