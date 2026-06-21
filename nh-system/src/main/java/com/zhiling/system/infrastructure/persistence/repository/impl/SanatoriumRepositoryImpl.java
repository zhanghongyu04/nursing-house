package com.zhiling.system.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.SanatoriumPageQueryDto;
import com.zhiling.model.entity.Sanatorium;
import com.zhiling.model.vo.RegionSanaCountVo;
import com.zhiling.system.application.repository.SanatoriumRepository;
import com.zhiling.system.infrastructure.persistence.command.SanatoriumCommandService;
import com.zhiling.system.infrastructure.persistence.query.SanatoriumQueryService;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 机构仓储实现。
 *
 * 委托给 SanatoriumQueryService 和 SanatoriumCommandService，
 * 实现 application 层定义的 SanatoriumRepository 契约。
 *
 * @author zhanghongyu
 */
@Repository
public class SanatoriumRepositoryImpl implements SanatoriumRepository {

    private final SanatoriumQueryService sanatoriumQueryService;
    private final SanatoriumCommandService sanatoriumCommandService;

    public SanatoriumRepositoryImpl(SanatoriumQueryService sanatoriumQueryService,
                                     SanatoriumCommandService sanatoriumCommandService) {
        this.sanatoriumQueryService = sanatoriumQueryService;
        this.sanatoriumCommandService = sanatoriumCommandService;
    }

    // === 查询操作 ===

    /**
     * 方法：listAll
     *
     * @author zhanghongyu
     */
    @Override
    public List<Sanatorium> listAll() {
        return sanatoriumQueryService.listAll();
    }

    /**
     * 方法：listByIds
     *
     * @author zhanghongyu
     */
    @Override
    public List<Sanatorium> listByIds(Set<Long> ids) {
        return sanatoriumQueryService.listByIds(ids);
    }

    /**
     * 方法：selectIdByName
     *
     * @author zhanghongyu
     */
    @Override
    public Long selectIdByName(String sanaName) {
        return sanatoriumQueryService.selectIdByName(sanaName);
    }

    /**
     * 方法：getNameById
     *
     * @author zhanghongyu
     */
    @Override
    public String getNameById(Long sanaId) {
        return sanatoriumQueryService.getNameById(sanaId);
    }

    /**
     * 方法：selectById
     *
     * @author zhanghongyu
     */
    @Override
    public Sanatorium selectById(Long id) {
        return sanatoriumQueryService.selectById(id);
    }

    /**
     * 方法：selectByExactName
     *
     * @author zhanghongyu
     */
    @Override
    public Sanatorium selectByExactName(String sanaName) {
        return sanatoriumQueryService.selectByExactName(sanaName);
    }

    @Override
    public boolean existsByUscc(String uscc) {
        return sanatoriumQueryService.existsByUscc(uscc);
    }

    /**
     * 方法：searchByKeyword
     *
     * @author zhanghongyu
     */
    @Override
    public List<Sanatorium> searchByKeyword(String keyword, Integer limit, Set<Long> scopeIds) {
        return sanatoriumQueryService.searchByKeyword(keyword, limit, scopeIds);
    }

    /**
     * 方法：count
     *
     * @author zhanghongyu
     */
    @Override
    public Integer count() {
        return sanatoriumQueryService.count();
    }

    /**
     * 方法：countNurse
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countNurse() {
        return sanatoriumQueryService.countNurse();
    }

    /**
     * 方法：countMedicine
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countMedicine() {
        return sanatoriumQueryService.countMedicine();
    }

    /**
     * 方法：countAffiliation
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countAffiliation() {
        return sanatoriumQueryService.countAffiliation();
    }

    /**
     * 方法：countBedInUse
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countBedInUse() {
        return sanatoriumQueryService.countBedInUse();
    }

    /**
     * 方法：countBed
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countBed() {
        return sanatoriumQueryService.countBed();
    }

    /**
     * 方法：listRegionSanaCount
     *
     * @author zhanghongyu
     */
    @Override
    public List<RegionSanaCountVo> listRegionSanaCount() {
        return sanatoriumQueryService.listRegionSanaCount();
    }

    /**
     * 方法：listRegionSanaCountBySanaId
     *
     * @author zhanghongyu
     */
    @Override
    public List<RegionSanaCountVo> listRegionSanaCountBySanaId(Long sanaId) {
        return sanatoriumQueryService.listRegionSanaCountBySanaId(sanaId);
    }

    /**
     * 方法：selectPage
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<Sanatorium> selectPage(Page<Sanatorium> page, SanatoriumPageQueryDto dto) {
        return sanatoriumQueryService.selectPage(page, dto);
    }

    // === 命令操作 ===

    /**
     * 方法：insert
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insert(Sanatorium sanatorium) {
        return sanatoriumCommandService.insert(sanatorium);
    }

    /**
     * 方法：deleteById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean deleteById(Long id) {
        return sanatoriumCommandService.deleteById(id);
    }

    /**
     * 方法：updateById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updateById(Sanatorium sanatorium) {
        return sanatoriumCommandService.updateById(sanatorium);
    }
}