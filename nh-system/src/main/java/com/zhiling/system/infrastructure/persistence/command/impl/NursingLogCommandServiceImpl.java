package com.zhiling.system.infrastructure.persistence.command.impl;

import com.zhiling.model.entity.NursingLog;
import com.zhiling.system.infrastructure.persistence.command.NursingLogCommandService;
import com.zhiling.system.infrastructure.persistence.mapper.NursingLogMapper;
import org.springframework.stereotype.Service;

/**
 * 护理日志命令服务实现
 *
 * @author zhanghongyu
 */
@Service
public class NursingLogCommandServiceImpl implements NursingLogCommandService {

    private final NursingLogMapper nursingLogMapper;

    /**
     * 构造器：NursingLogCommandServiceImpl
     *
     * @author zhanghongyu
     */
    public NursingLogCommandServiceImpl(NursingLogMapper nursingLogMapper) {
        this.nursingLogMapper = nursingLogMapper;
    }

    /**
     * 方法：insert
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insert(NursingLog nursingLog) {
        return nursingLogMapper.insert(nursingLog) > 0;
    }

    /**
     * 方法：updateById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updateById(NursingLog nursingLog) {
        return nursingLogMapper.updateById(nursingLog) > 0;
    }
}