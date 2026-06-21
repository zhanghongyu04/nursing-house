package com.zhiling.system.infrastructure.persistence.query.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.SanatoriumPageQueryDto;
import com.zhiling.model.entity.Sanatorium;
import com.zhiling.system.infrastructure.persistence.mapper.SanatoriumMapper;
import com.zhiling.system.infrastructure.persistence.query.SanatoriumQueryService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 机构查询服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class SanatoriumQueryServiceImpl implements SanatoriumQueryService {

    private final SanatoriumMapper sanatoriumMapper;

    /**
     * 构造器：SanatoriumQueryServiceImpl
     *
     * @author zhanghongyu
     */
    public SanatoriumQueryServiceImpl(SanatoriumMapper sanatoriumMapper) {
        this.sanatoriumMapper = sanatoriumMapper;
    }

    /**
     * 方法：listAll
     *
     * @author zhanghongyu
     */
    @Override
    public List<Sanatorium> listAll() {
        return sanatoriumMapper.list();
    }

    /**
     * 方法：listByIds
     *
     * @author zhanghongyu
     */
    @Override
    public List<Sanatorium> listByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return sanatoriumMapper.selectBatchIds(ids);
    }

    /**
     * 方法：selectIdByName
     *
     * @author zhanghongyu
     */
    @Override
    public Long selectIdByName(String sanaName) {
        return sanatoriumMapper.selectIdByName(sanaName);
    }

    /**
     * 方法：getNameById
     *
     * @author zhanghongyu
     */
    @Override
    public String getNameById(Long sanaId) {
        return sanatoriumMapper.getName(sanaId);
    }

    /**
     * 方法：selectById
     *
     * @author zhanghongyu
     */
    @Override
    public Sanatorium selectById(Long id) {
        if (id == null) {
            return null;
        }
        return sanatoriumMapper.selectById(id);
    }

    /**
     * 方法：selectByExactName
     *
     * @author zhanghongyu
     */
    @Override
    public Sanatorium selectByExactName(String sanaName) {
        if (sanaName == null || sanaName.trim().isEmpty()) {
            return null;
        }
        return sanatoriumMapper.selectOne(
                new LambdaQueryWrapper<Sanatorium>()
                        .eq(Sanatorium::getSanaName, sanaName)
                        .last("LIMIT 1")
        );
    }

    @Override
    public boolean existsByUscc(String uscc) {
        if (uscc == null || uscc.trim().isEmpty()) {
            return false;
        }
        Integer count = sanatoriumMapper.selectCountByUscc(uscc);
        return count != null && count > 0;
    }

    /**
     * 方法：searchByKeyword
     *
     * @author zhanghongyu
     */
    @Override
    public List<Sanatorium> searchByKeyword(String keyword, Integer limit, Set<Long> scopeIds) {
        int safeLimit = normalizeLimit(limit);
        LambdaQueryWrapper<Sanatorium> wrapper = new LambdaQueryWrapper<Sanatorium>()
                .like(Sanatorium::getSanaName, keyword)
                .orderByAsc(Sanatorium::getSanaName)
                .last("LIMIT " + safeLimit);
        if (scopeIds != null && !scopeIds.isEmpty()) {
            wrapper.in(Sanatorium::getId, scopeIds);
        }
        return sanatoriumMapper.selectList(wrapper);
    }

    /**
     * 方法：normalizeLimit
     *
     * @author zhanghongyu
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 5;
        }
        return Math.min(limit, 10);
    }

    /**
     * 方法：count
     *
     * @author zhanghongyu
     */
    @Override
    public Integer count() {
        Integer count = sanatoriumMapper.selectCount();
        return count != null ? count : 0;
    }

    /**
     * 方法：countNurse
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countNurse() {
        Integer count = sanatoriumMapper.selectNurseCount();
        return count != null ? count : 0;
    }

    /**
     * 方法：countMedicine
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countMedicine() {
        Integer count = sanatoriumMapper.selectMedicineCount();
        return count != null ? count : 0;
    }

    /**
     * 方法：countAffiliation
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countAffiliation() {
        Integer count = sanatoriumMapper.selectAffiliationCount();
        return count != null ? count : 0;
    }

    /**
     * 方法：countBedInUse
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countBedInUse() {
        Integer count = sanatoriumMapper.selectUseBedCount();
        return count != null ? count : 0;
    }

    /**
     * 方法：countBed
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countBed() {
        Integer count = sanatoriumMapper.selectBedCount();
        return count != null ? count : 0;
    }

    /**
     * 方法：listRegionSanaCount
     *
     * @author zhanghongyu
     */
    @Override
    public List<com.zhiling.model.vo.RegionSanaCountVo> listRegionSanaCount() {
        return sanatoriumMapper.listRegionSanaCount();
    }

    /**
     * 方法：listRegionSanaCountBySanaId
     *
     * @author zhanghongyu
     */
    @Override
    public List<com.zhiling.model.vo.RegionSanaCountVo> listRegionSanaCountBySanaId(Long sanaId) {
        if (sanaId == null) {
            return Collections.emptyList();
        }
        return sanatoriumMapper.listRegionSanaCountBySanaId(sanaId);
    }

    /**
     * 方法：selectPage
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<Sanatorium> selectPage(Page<Sanatorium> page, SanatoriumPageQueryDto dto) {
        return sanatoriumMapper.page(page, dto);
    }
}
