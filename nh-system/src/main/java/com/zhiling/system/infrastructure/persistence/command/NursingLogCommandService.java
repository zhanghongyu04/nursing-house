package com.zhiling.system.infrastructure.persistence.command;

import com.zhiling.model.entity.NursingLog;

/**
 * 护理日志命令服务
 * @author zhanghongyu
 */
public interface NursingLogCommandService {

    boolean insert(NursingLog nursingLog);

    boolean updateById(NursingLog nursingLog);
}
