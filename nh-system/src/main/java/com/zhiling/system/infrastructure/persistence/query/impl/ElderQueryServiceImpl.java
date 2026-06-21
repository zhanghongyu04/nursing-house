package com.zhiling.system.infrastructure.persistence.query.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.ElderPageQueryDto;
import com.zhiling.model.dto.SanatoriumDetailPageQueryDto;
import com.zhiling.model.entity.Elder;
import com.zhiling.system.infrastructure.persistence.mapper.ElderMapper;
import com.zhiling.system.infrastructure.persistence.query.ElderQueryService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 老人查询服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class ElderQueryServiceImpl implements ElderQueryService {

    private final ElderMapper elderMapper;

    /**
     * 构造器：ElderQueryServiceImpl
     *
     * @author zhanghongyu
     */
    public ElderQueryServiceImpl(ElderMapper elderMapper) {
        this.elderMapper = elderMapper;
    }

    /**
     * 方法：count
     *
     * @author zhanghongyu
     */
    @Override
    public Integer count() {
        Integer count = elderMapper.selectCount();
        return count != null ? count : 0;
    }

    /**
     * 方法：countBySanaId
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countBySanaId(Long sanaId) {
        if (sanaId == null) {
            return 0;
        }
        Integer count = elderMapper.selectCountBySanaId(sanaId);
        return count != null ? count : 0;
    }

    /**
     * 方法：countBySanaIds
     *
     * @author zhanghongyu
     */
    @Override
    public Integer countBySanaIds(Set<Long> sanaIds) {
        if (sanaIds == null || sanaIds.isEmpty()) {
            return 0;
        }
        Integer count = elderMapper.selectCountBySanaIds(sanaIds);
        return count != null ? count : 0;
    }

    /**
     * 方法：queryElderDistribution
     *
     * @author zhanghongyu
     */
    @Override
    public List<Map<String, Object>> queryElderDistribution(String sanaName) {
        if (sanaName == null || sanaName.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return elderMapper.queryElderDistribution(sanaName);
    }

    /**
     * 方法：queryAllElderDistribution
     *
     * @author zhanghongyu
     */
    @Override
    public List<Map<String, Object>> queryAllElderDistribution() {
        return elderMapper.queryAllElderDistribution();
    }

    /**
     * 方法：queryElderDistributionBySanaId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Map<String, Object>> queryElderDistributionBySanaId(Long sanaId) {
        if (sanaId == null) {
            return Collections.emptyList();
        }
        return elderMapper.queryElderDistributionBySanaId(sanaId);
    }

    /**
     * 方法：selectById
     *
     * @author zhanghongyu
     */
    @Override
    public Elder selectById(Long id) {
        if (id == null) {
            return null;
        }
        return elderMapper.selectById(id);
    }

    /**
     * 方法：selectPage
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<Elder> selectPage(Page<Elder> page, ElderPageQueryDto dto) {
        return elderMapper.page(page, dto);
    }

    /**
     * 方法：selectPageSanaElderList
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<Elder> selectPageSanaElderList(Page<Elder> page, SanatoriumDetailPageQueryDto dto) {
        return elderMapper.pageSanaElderList(page, dto);
    }

    /**
     * 方法：listAll
     *
     * @author zhanghongyu
     */
    @Override
    public List<Elder> listAll() {
        List<Elder> elders = elderMapper.list();
        return elders != null ? elders : Collections.emptyList();
    }
}