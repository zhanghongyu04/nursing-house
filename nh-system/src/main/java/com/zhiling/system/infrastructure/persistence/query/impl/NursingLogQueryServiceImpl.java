package com.zhiling.system.infrastructure.persistence.query.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.NursingLogMyPageQueryDto;
import com.zhiling.model.dto.NursingLogPageQueryDto;
import com.zhiling.model.entity.NursingLog;
import com.zhiling.system.infrastructure.persistence.mapper.NursingLogMapper;
import com.zhiling.system.infrastructure.persistence.query.NursingLogQueryService;
import org.springframework.stereotype.Service;

/**
 * 护理日志查询服务实现
 *
 * @author zhanghongyu
 */
@Service
public class NursingLogQueryServiceImpl implements NursingLogQueryService {

    private final NursingLogMapper nursingLogMapper;

    /**
     * 构造器：NursingLogQueryServiceImpl
     *
     * @author zhanghongyu
     */
    public NursingLogQueryServiceImpl(NursingLogMapper nursingLogMapper) {
        this.nursingLogMapper = nursingLogMapper;
    }

    /**
     * 方法：selectById
     *
     * @author zhanghongyu
     */
    @Override
    public NursingLog selectById(Long id) {
        if (id == null) {
            return null;
        }
        return nursingLogMapper.selectById(id);
    }

    /**
     * 方法：selectByTaskIdAndNurseUserId
     *
     * @author zhanghongyu
     */
    @Override
    public NursingLog selectByTaskIdAndNurseUserId(Long taskId, Long nurseUserId) {
        if (taskId == null || nurseUserId == null) {
            return null;
        }
        return nursingLogMapper.selectOne(
                new LambdaQueryWrapper<NursingLog>()
                        .eq(NursingLog::getTaskId, taskId)
                        .eq(NursingLog::getNurseUserId, nurseUserId)
                        .last("LIMIT 1")
        );
    }

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<NursingLog> page(Page<NursingLog> page, NursingLogPageQueryDto dto) {
        return nursingLogMapper.page(page, dto);
    }

    /**
     * 方法：myPage
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<NursingLog> myPage(Page<NursingLog> page, NursingLogMyPageQueryDto dto) {
        return nursingLogMapper.myPage(page, dto);
    }
}