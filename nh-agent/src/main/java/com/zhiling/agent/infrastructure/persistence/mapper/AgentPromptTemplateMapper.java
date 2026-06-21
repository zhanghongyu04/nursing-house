package com.zhiling.agent.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiling.agent.infrastructure.persistence.entity.AgentPromptTemplate;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Agent 提示词配置 Mapper
 *
 * @author zhanghongyu
 */
@Mapper
public interface AgentPromptTemplateMapper extends BaseMapper<AgentPromptTemplate> {

    /**
     * 查询指定提示词名称的最新启用版本号
     */
    default Integer selectLatestActiveVersion(String promptName) {
        AgentPromptTemplate latest = selectOne(new LambdaQueryWrapper<AgentPromptTemplate>()
                .eq(AgentPromptTemplate::getPromptName, promptName)
                .eq(AgentPromptTemplate::getStatus, 0)
                .orderByDesc(AgentPromptTemplate::getVersion)
                .last("LIMIT 1")
                .select(AgentPromptTemplate::getVersion));
        return latest == null ? null : latest.getVersion();
    }

    /**
     * 查询指定名称+版本的启用提示词片段（按索引升序）
     */
    default List<AgentPromptTemplate> selectActiveByNameAndVersion(String promptName, Integer version) {
        return selectList(new LambdaQueryWrapper<AgentPromptTemplate>()
                .eq(AgentPromptTemplate::getPromptName, promptName)
                .eq(AgentPromptTemplate::getVersion, version)
                .eq(AgentPromptTemplate::getStatus, 0)
                .orderByAsc(AgentPromptTemplate::getPromptIndex));
    }

    /**
     * 查询所有提示词名称。
     */
    default List<String> selectPromptNames() {
        return selectObjs(new QueryWrapper<AgentPromptTemplate>()
                .select("DISTINCT prompt_name")
                .orderByAsc("prompt_name")).stream()
                .map(String::valueOf)
                .toList();
    }

    /**
     * 查询指定提示词的版本摘要。
     */
    default List<AgentPromptTemplate> selectVersionSummaryRows(String promptName) {
        QueryWrapper<AgentPromptTemplate> wrapper = new QueryWrapper<>();
        wrapper.select("prompt_name", "version", "MIN(status) AS status",
                        "COUNT(*) AS segment_count", "MAX(update_time) AS update_time")
                .groupBy("prompt_name", "version")
                .orderByDesc("version")
                .orderByAsc("MIN(status)");
        if (promptName != null && !promptName.isBlank()) {
            wrapper.eq("prompt_name", promptName);
        }
        return selectList(wrapper);
    }

    /**
     * 查询指定名称+版本的提示词片段（包含停用片段）。
     */
    default List<AgentPromptTemplate> selectByNameAndVersion(String promptName, Integer version) {
        return selectList(new LambdaQueryWrapper<AgentPromptTemplate>()
                .eq(AgentPromptTemplate::getPromptName, promptName)
                .eq(AgentPromptTemplate::getVersion, version)
                .orderByAsc(AgentPromptTemplate::getPromptIndex));
    }

    /**
     * 查询指定提示词的最大版本号。
     */
    default Integer selectMaxVersion(String promptName) {
        AgentPromptTemplate latest = selectOne(new LambdaQueryWrapper<AgentPromptTemplate>()
                .eq(AgentPromptTemplate::getPromptName, promptName)
                .orderByDesc(AgentPromptTemplate::getVersion)
                .last("LIMIT 1")
                .select(AgentPromptTemplate::getVersion));
        return latest == null ? null : latest.getVersion();
    }
}


