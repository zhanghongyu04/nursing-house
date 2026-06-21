package com.zhiling.system.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.NursingLogMyPageQueryDto;
import com.zhiling.model.dto.NursingLogPageQueryDto;
import com.zhiling.model.entity.NursingLog;
import com.zhiling.system.application.repository.NursingLogRepository;
import com.zhiling.system.infrastructure.persistence.command.NursingLogCommandService;
import com.zhiling.system.infrastructure.persistence.query.NursingLogQueryService;
import org.springframework.stereotype.Repository;

/**
 * 护理日志仓储实现
 *
 * @author zhanghongyu
 */
@Repository
public class NursingLogRepositoryImpl implements NursingLogRepository {

    private final NursingLogQueryService nursingLogQueryService;
    private final NursingLogCommandService nursingLogCommandService;

    public NursingLogRepositoryImpl(NursingLogQueryService nursingLogQueryService,
                                    NursingLogCommandService nursingLogCommandService) {
        this.nursingLogQueryService = nursingLogQueryService;
        this.nursingLogCommandService = nursingLogCommandService;
    }

    /**
     * 方法：selectById
     *
     * @author zhanghongyu
     */
    @Override
    public NursingLog selectById(Long id) {
        return nursingLogQueryService.selectById(id);
    }

    /**
     * 方法：selectByTaskIdAndNurseUserId
     *
     * @author zhanghongyu
     */
    @Override
    public NursingLog selectByTaskIdAndNurseUserId(Long taskId, Long nurseUserId) {
        return nursingLogQueryService.selectByTaskIdAndNurseUserId(taskId, nurseUserId);
    }

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<NursingLog> page(Page<NursingLog> page, NursingLogPageQueryDto dto) {
        return nursingLogQueryService.page(page, dto);
    }

    /**
     * 方法：myPage
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<NursingLog> myPage(Page<NursingLog> page, NursingLogMyPageQueryDto dto) {
        return nursingLogQueryService.myPage(page, dto);
    }

    /**
     * 方法：insert
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insert(NursingLog nursingLog) {
        return nursingLogCommandService.insert(nursingLog);
    }

    /**
     * 方法：updateById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updateById(NursingLog nursingLog) {
        return nursingLogCommandService.updateById(nursingLog);
    }
}