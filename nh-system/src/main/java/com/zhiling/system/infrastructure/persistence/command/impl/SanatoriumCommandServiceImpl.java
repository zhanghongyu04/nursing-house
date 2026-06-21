package com.zhiling.system.infrastructure.persistence.command.impl;

import com.zhiling.model.entity.Sanatorium;
import com.zhiling.system.infrastructure.persistence.command.SanatoriumCommandService;
import com.zhiling.system.infrastructure.persistence.mapper.SanatoriumMapper;
import org.springframework.stereotype.Service;

/**
 * 机构命令服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class SanatoriumCommandServiceImpl implements SanatoriumCommandService {

    private final SanatoriumMapper sanatoriumMapper;

    /**
     * 构造器：SanatoriumCommandServiceImpl
     *
     * @author zhanghongyu
     */
    public SanatoriumCommandServiceImpl(SanatoriumMapper sanatoriumMapper) {
        this.sanatoriumMapper = sanatoriumMapper;
    }

    /**
     * 方法：insert
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insert(Sanatorium sanatorium) {
        return sanatoriumMapper.insert(sanatorium) > 0;
    }

    /**
     * 方法：updateById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updateById(Sanatorium sanatorium) {
        return sanatoriumMapper.updateById(sanatorium) > 0;
    }

    /**
     * 方法：deleteById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean deleteById(Long id) {
        return sanatoriumMapper.deleteById(id) > 0;
    }
}