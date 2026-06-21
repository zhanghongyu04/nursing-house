package com.zhiling.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiling.agent.application.prompt.model.PromptTemplateChangeLogCommand;
import com.zhiling.agent.application.prompt.model.PromptTemplateLogCommand;
import com.zhiling.agent.application.prompt.model.PromptTemplateLogView;
import com.zhiling.agent.application.repository.PromptTemplateLogRepository;
import com.zhiling.agent.infrastructure.persistence.entity.AgentPromptTemplateLog;
import com.zhiling.agent.infrastructure.persistence.mapper.AgentPromptTemplateLogMapper;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * PromptTemplateLogRepositoryImpl
 *
 * @author zhanghongyu
 */
public class PromptTemplateLogRepositoryImpl implements PromptTemplateLogRepository {

    private final AgentPromptTemplateLogMapper promptTemplateLogMapper;

    /**
     * 构造器：PromptTemplateLogRepositoryImpl
     *
     * @author zhanghongyu
     */
    public PromptTemplateLogRepositoryImpl(AgentPromptTemplateLogMapper promptTemplateLogMapper) {
        this.promptTemplateLogMapper = promptTemplateLogMapper;
    }

    /**
     * 方法：saveLoadLog
     *
     * @author zhanghongyu
     */
    @Override
    public void saveLoadLog(PromptTemplateLogCommand command) {
        AgentPromptTemplateLog logEntity = new AgentPromptTemplateLog();
        logEntity.setPromptId(command.promptId());
        logEntity.setPromptName(command.promptName());
        logEntity.setPromptIndex(0);
        logEntity.setOldVersion(null);
        logEntity.setNewVersion(command.version());
        logEntity.setOldContent(command.oldContent());
        logEntity.setNewContent(command.newContent());
        logEntity.setOperationType(StrUtil.blankToDefault(command.operationType(), "LOAD_REDIS"));
        logEntity.setOperator(StrUtil.blankToDefault(command.operator(), "system"));
        logEntity.setRemark(StrUtil.blankToDefault(command.remark(), "应用启动时自动同步提示词到Redis"));
        promptTemplateLogMapper.insert(logEntity);
    }

    @Override
    public void saveChangeLog(PromptTemplateChangeLogCommand command) {
        AgentPromptTemplateLog logEntity = new AgentPromptTemplateLog();
        logEntity.setPromptId(command.promptId());
        logEntity.setPromptName(command.promptName());
        logEntity.setPromptIndex(command.promptIndex() == null ? 0 : command.promptIndex());
        logEntity.setOldVersion(command.oldVersion());
        logEntity.setNewVersion(command.newVersion());
        logEntity.setOldContent(command.oldContent());
        logEntity.setNewContent(command.newContent());
        logEntity.setOperationType(StrUtil.blankToDefault(command.operationType(), "UPDATE_SEGMENT"));
        logEntity.setOperator(StrUtil.blankToDefault(command.operator(), "unknown"));
        logEntity.setRemark(StrUtil.blankToDefault(command.remark(), "控制台提示词变更"));
        promptTemplateLogMapper.insert(logEntity);
    }

    @Override
    public List<PromptTemplateLogView> listLogs(String promptName, String operationType, int limit) {
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        LambdaQueryWrapper<AgentPromptTemplateLog> wrapper = new LambdaQueryWrapper<AgentPromptTemplateLog>()
                .orderByDesc(AgentPromptTemplateLog::getCreateTime)
                .last("LIMIT " + safeLimit);
        if (StrUtil.isNotBlank(promptName)) {
            wrapper.eq(AgentPromptTemplateLog::getPromptName, promptName);
        }
        if (StrUtil.isNotBlank(operationType)) {
            wrapper.eq(AgentPromptTemplateLog::getOperationType, operationType);
        }
        return promptTemplateLogMapper.selectList(wrapper).stream()
                .map(this::toView)
                .toList();
    }

    private PromptTemplateLogView toView(AgentPromptTemplateLog entity) {
        return new PromptTemplateLogView(
                entity.getId(),
                entity.getPromptId(),
                entity.getPromptName(),
                entity.getPromptIndex(),
                entity.getOldVersion(),
                entity.getNewVersion(),
                entity.getOldContent(),
                entity.getNewContent(),
                entity.getOperationType(),
                entity.getOperator(),
                entity.getRemark(),
                entity.getCreateTime());
    }
}
