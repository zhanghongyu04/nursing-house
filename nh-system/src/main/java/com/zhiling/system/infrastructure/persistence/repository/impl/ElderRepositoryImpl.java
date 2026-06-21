package com.zhiling.system.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.ElderDto;
import com.zhiling.model.dto.ElderPageQueryDto;
import com.zhiling.model.dto.SanatoriumDetailPageQueryDto;
import com.zhiling.model.entity.Elder;
import com.zhiling.system.application.repository.ElderRepository;
import com.zhiling.system.infrastructure.persistence.command.ElderCommandService;
import com.zhiling.system.infrastructure.persistence.query.ElderQueryService;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 老人仓储实现。
 *
 * 委托给 ElderQueryService 和 ElderCommandService，
 * 实现 application 层定义的 ElderRepository 契约。
 *
 * @author zhanghongyu
 */
@Repository
public class ElderRepositoryImpl implements ElderRepository {

    private final ElderQueryService elderQueryService;
    private final ElderCommandService elderCommandService;

    public ElderRepositoryImpl(ElderQueryService elderQueryService,
                                ElderCommandService elderCommandService) {
        this.elderQueryService = elderQueryService;
        this.elderCommandService = elderCommandService;
    }

    // === 查询操作 ===

    /**
     * 方法：count
     *
     * @author zhanghongyu
     */
    @Override
    public Integer count() {
        return elderQueryService.count();
    }

    /**
     * 方法：countBySanaId
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countBySanaId(Long sanaId) {
        return elderQueryService.countBySanaId(sanaId);
    }

    /**
     * 方法：countBySanaIds
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countBySanaIds(Set<Long> sanaIds) {
        return elderQueryService.countBySanaIds(sanaIds);
    }

    /**
     * 方法：queryElderDistribution
     *
     * @author zhanghongyu
     */
    @Override
    public List<Map<String, Object>> queryElderDistribution(String sanaName) {
        return elderQueryService.queryElderDistribution(sanaName);
    }

    /**
     * 方法：queryAllElderDistribution
     *
     * @author zhanghongyu
     */
    @Override
    public List<Map<String, Object>> queryAllElderDistribution() {
        return elderQueryService.queryAllElderDistribution();
    }

    /**
     * 方法：queryElderDistributionBySanaId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Map<String, Object>> queryElderDistributionBySanaId(Long sanaId) {
        return elderQueryService.queryElderDistributionBySanaId(sanaId);
    }

    /**
     * 方法：selectById
     *
     * @author zhanghongyu
     */
    @Override
    public Elder selectById(Long id) {
        return elderQueryService.selectById(id);
    }

    /**
     * 方法：selectPage
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<Elder> selectPage(Page<Elder> page, ElderPageQueryDto dto) {
        return elderQueryService.selectPage(page, dto);
    }

    /**
     * 方法：selectPageSanaElderList
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<Elder> selectPageSanaElderList(Page<Elder> page, SanatoriumDetailPageQueryDto dto) {
        return elderQueryService.selectPageSanaElderList(page, dto);
    }

    /**
     * 方法：listAll
     *
     * @author zhanghongyu
     */
    @Override
    public List<Elder> listAll() {
        return elderQueryService.listAll();
    }

    // === 命令操作 ===

    /**
     * 方法：insert
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insert(Elder elder) {
        return elderCommandService.insert(elder);
    }

    /**
     * 方法：removeById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean removeById(Long id) {
        return elderCommandService.removeById(id);
    }

    /**
     * 方法：update
     *
     * @author zhanghongyu
     */
    @Override
    public boolean update(ElderDto elderDto) {
        return elderCommandService.update(elderDto);
    }
}