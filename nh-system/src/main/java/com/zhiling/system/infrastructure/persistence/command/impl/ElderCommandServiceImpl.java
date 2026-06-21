package com.zhiling.system.infrastructure.persistence.command.impl;

import com.zhiling.model.dto.ElderDto;
import com.zhiling.model.entity.Elder;
import com.zhiling.system.infrastructure.persistence.command.ElderCommandService;
import com.zhiling.system.infrastructure.persistence.mapper.ElderMapper;
import org.springframework.stereotype.Service;

/**
 * 老人命令服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class ElderCommandServiceImpl implements ElderCommandService {

    private final ElderMapper elderMapper;

    /**
     * 构造器：ElderCommandServiceImpl
     *
     * @author zhanghongyu
     */
    public ElderCommandServiceImpl(ElderMapper elderMapper) {
        this.elderMapper = elderMapper;
    }

    /**
     * 方法：insert
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insert(Elder elder) {
        return elderMapper.add(elder);
    }

    /**
     * 方法：removeById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean removeById(Long id) {
        return elderMapper.removeById(id);
    }

    /**
     * 方法：update
     *
     * @author zhanghongyu
     */
    @Override
    public boolean update(ElderDto elderDto) {
        return elderMapper.update(elderDto);
    }
}