package com.zhiling.system.infrastructure.persistence.query.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.NursingTaskMyPageQueryDto;
import com.zhiling.model.dto.NursingTaskPageQueryDto;
import com.zhiling.model.entity.NursingTask;
import com.zhiling.system.infrastructure.persistence.mapper.NursingTaskMapper;
import com.zhiling.system.infrastructure.persistence.query.NursingTaskQueryService;
import org.springframework.stereotype.Service;

/**
 * 护理任务查询服务实现
 *
 * @author zhanghongyu
 */
@Service
public class NursingTaskQueryServiceImpl implements NursingTaskQueryService {

    private final NursingTaskMapper nursingTaskMapper;

    /**
     * 构造器：NursingTaskQueryServiceImpl
     *
     * @author zhanghongyu
     */
    public NursingTaskQueryServiceImpl(NursingTaskMapper nursingTaskMapper) {
        this.nursingTaskMapper = nursingTaskMapper;
    }

    /**
     * 方法：selectById
     *
     * @author zhanghongyu
     */
    @Override
    public NursingTask selectById(Long id) {
        if (id == null) {
            return null;
        }
        return nursingTaskMapper.selectOne(
                new LambdaQueryWrapper<NursingTask>()
                        .eq(NursingTask::getId, id)
                        .eq(NursingTask::getDeleted, 0)
        );
    }

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<NursingTask> page(Page<NursingTask> page, NursingTaskPageQueryDto dto) {
        return nursingTaskMapper.page(page, dto);
    }

    /**
     * 方法：myPage
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<NursingTask> myPage(Page<NursingTask> page, NursingTaskMyPageQueryDto dto) {
        return nursingTaskMapper.myPage(page, dto);
    }
}