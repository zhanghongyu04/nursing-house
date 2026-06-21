package com.zhiling.agent.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiling.agent.infrastructure.persistence.entity.AgentPromptTemplateLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 提示词变更日志 Mapper
 *
 * @author zhanghongyu
 */
@Mapper
public interface AgentPromptTemplateLogMapper extends BaseMapper<AgentPromptTemplateLog> {
}


