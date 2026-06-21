package com.zhiling.system.infrastructure.persistence.command;

import com.zhiling.model.dto.ElderDto;
import com.zhiling.model.entity.Elder;

/**
 * 老人命令服务接口。
 *
 * @author zhanghongyu
 */
public interface ElderCommandService {

    boolean insert(Elder elder);

    boolean removeById(Long id);

    boolean update(ElderDto elderDto);
}
