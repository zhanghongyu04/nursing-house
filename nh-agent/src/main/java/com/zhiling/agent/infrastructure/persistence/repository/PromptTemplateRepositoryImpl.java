package com.zhiling.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhiling.agent.application.prompt.model.PromptTemplateSegment;
import com.zhiling.agent.application.prompt.model.PromptTemplateDetail;
import com.zhiling.agent.application.prompt.model.PromptTemplateSegmentEdit;
import com.zhiling.agent.application.prompt.model.PromptTemplateSummary;
import com.zhiling.agent.application.repository.PromptTemplateRepository;
import com.zhiling.agent.infrastructure.persistence.entity.AgentPromptTemplate;
import com.zhiling.agent.infrastructure.persistence.mapper.AgentPromptTemplateMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * PromptTemplateRepositoryImpl
 *
 * @author zhanghongyu
 */
public class PromptTemplateRepositoryImpl implements PromptTemplateRepository {

    private final AgentPromptTemplateMapper promptTemplateMapper;

    /**
     * 构造器：PromptTemplateRepositoryImpl
     *
     * @author zhanghongyu
     */
    public PromptTemplateRepositoryImpl(AgentPromptTemplateMapper promptTemplateMapper) {
        this.promptTemplateMapper = promptTemplateMapper;
    }

    /**
     * 方法：selectLatestActiveVersion
     *
     * @author zhanghongyu
     */
    @Override
    public Integer selectLatestActiveVersion(String promptName) {
        return promptTemplateMapper.selectLatestActiveVersion(promptName);
    }

    /**
     * 方法：selectActiveByNameAndVersion
     *
     * @author zhanghongyu
     */
    @Override
    public List<PromptTemplateSegment> selectActiveByNameAndVersion(String promptName, Integer version) {
        return promptTemplateMapper.selectActiveByNameAndVersion(promptName, version).stream()
                .map(segment -> new PromptTemplateSegment(segment.getId(), segment.getPromptContent()))
                .toList();
    }

    @Override
    public List<String> selectPromptNames() {
        return promptTemplateMapper.selectPromptNames();
    }

    @Override
    public List<PromptTemplateSummary> selectVersionSummaries(String promptName) {
        return promptTemplateMapper.selectVersionSummaryRows(promptName).stream()
                .map(row -> new PromptTemplateSummary(
                        row.getPromptName(),
                        row.getVersion(),
                        row.getStatus(),
                        row.getSegmentCount(),
                        row.getUpdateTime()))
                .toList();
    }

    @Override
    public List<PromptTemplateDetail> selectDetailsByNameAndVersion(String promptName, Integer version) {
        return promptTemplateMapper.selectByNameAndVersion(promptName, version).stream()
                .map(this::toDetail)
                .toList();
    }

    @Override
    public PromptTemplateDetail selectDetailById(Long id) {
        AgentPromptTemplate entity = promptTemplateMapper.selectById(id);
        return entity == null ? null : toDetail(entity);
    }

    @Override
    public Integer selectMaxVersion(String promptName) {
        return promptTemplateMapper.selectMaxVersion(promptName);
    }

    @Override
    public List<PromptTemplateDetail> insertVersion(String promptName, Integer version, List<PromptTemplateSegmentEdit> segments, Integer status) {
        for (PromptTemplateSegmentEdit segment : segments) {
            AgentPromptTemplate entity = new AgentPromptTemplate();
            entity.setPromptName(promptName);
            entity.setPromptIndex(segment.promptIndex());
            entity.setPromptContent(segment.promptContent());
            entity.setVersion(version);
            entity.setStatus(status);
            promptTemplateMapper.insert(entity);
        }
        return selectDetailsByNameAndVersion(promptName, version);
    }

    @Override
    public PromptTemplateDetail insertSegment(String promptName, Integer version, PromptTemplateSegmentEdit segment, Integer status) {
        AgentPromptTemplate entity = new AgentPromptTemplate();
        entity.setPromptName(promptName);
        entity.setPromptIndex(segment.promptIndex());
        entity.setPromptContent(segment.promptContent());
        entity.setVersion(version);
        entity.setStatus(status);
        promptTemplateMapper.insert(entity);
        return selectDetailById(entity.getId());
    }

    @Override
    public PromptTemplateDetail updateSegmentContent(Long id, String promptContent) {
        AgentPromptTemplate entity = new AgentPromptTemplate();
        entity.setId(id);
        entity.setPromptContent(promptContent);
        promptTemplateMapper.updateById(entity);
        return selectDetailById(id);
    }

    @Override
    public List<PromptTemplateDetail> updateVersionStatus(String promptName, Integer version, Integer status) {
        promptTemplateMapper.update(null, new LambdaUpdateWrapper<AgentPromptTemplate>()
                .eq(AgentPromptTemplate::getPromptName, promptName)
                .eq(AgentPromptTemplate::getVersion, version)
                .set(AgentPromptTemplate::getStatus, status));
        return selectDetailsByNameAndVersion(promptName, version);
    }

    @Override
    public void updateOtherVersionsStatus(String promptName, Integer version, Integer status) {
        promptTemplateMapper.update(null, new LambdaUpdateWrapper<AgentPromptTemplate>()
                .eq(AgentPromptTemplate::getPromptName, promptName)
                .ne(AgentPromptTemplate::getVersion, version)
                .set(AgentPromptTemplate::getStatus, status));
    }

    @Override
    public PromptTemplateDetail updateSegmentStatus(Long id, Integer status) {
        AgentPromptTemplate entity = new AgentPromptTemplate();
        entity.setId(id);
        entity.setStatus(status);
        promptTemplateMapper.updateById(entity);
        return selectDetailById(id);
    }

    private PromptTemplateDetail toDetail(AgentPromptTemplate entity) {
        return new PromptTemplateDetail(
                entity.getId(),
                entity.getPromptName(),
                entity.getPromptIndex(),
                entity.getPromptContent(),
                entity.getVersion(),
                entity.getStatus(),
                entity.getCreateTime(),
                entity.getUpdateTime());
    }
}
