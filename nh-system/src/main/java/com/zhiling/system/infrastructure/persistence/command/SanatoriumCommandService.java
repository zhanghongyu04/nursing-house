package com.zhiling.system.infrastructure.persistence.command;

import com.zhiling.model.entity.Sanatorium;

/**
 * 机构命令服务接口。
 *
 * @author zhanghongyu
 */
public interface SanatoriumCommandService {

    boolean insert(Sanatorium sanatorium);

    boolean updateById(Sanatorium sanatorium);

    boolean deleteById(Long id);
}
