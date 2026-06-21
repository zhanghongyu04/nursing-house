package com.zhiling.agent.application.prompt;

import cn.hutool.core.util.StrUtil;
import com.zhiling.agent.application.prompt.model.PromptTemplateLogCommand;
import com.zhiling.agent.application.prompt.model.PromptTemplateSegment;
import com.zhiling.agent.application.repository.PromptCachePort;
import com.zhiling.agent.application.repository.PromptTemplateLogRepository;
import com.zhiling.agent.application.repository.PromptTemplateRepository;
import com.zhiling.common.constant.PromptConstant;
import com.zhiling.framework.llm.service.AgentPromptTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 提示词加载应用服务。
 * 负责从数据库读取最新启用版本的提示词片段，合并后写入缓存并记录加载日志。
 *
 * @author zhanghongyu
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentPromptTemplateLoadService implements AgentPromptTemplateService {

    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptTemplateLogRepository promptTemplateLogRepository;
    private final PromptCachePort promptCachePort;
    private final PromptTemplateMerger promptTemplateMerger;

    /**
     * 将指定提示词加载到 Redis 缓存。
     *
     * @param promptName 提示词名称
     * @return 合并后的完整提示词内容
     * @throws IllegalArgumentException promptName 为空时抛出
     * @throws IllegalStateException    未找到可用版本或内容为空时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String loadPromptToRedis(String promptName) {
        return loadPromptToRedis(promptName, "LOAD_REDIS", "system", "应用启动时自动同步提示词到Redis");
    }

    @Transactional(rollbackFor = Exception.class)
    public String loadPromptToRedis(String promptName, String operationType, String operator, String remark) {
        if (StrUtil.isBlank(promptName)) {
            throw new IllegalArgumentException("promptName 不能为空");
        }

        Integer latestVersion = promptTemplateRepository.selectLatestActiveVersion(promptName);
        if (latestVersion == null) {
            throw new IllegalStateException("未在数据库中找到可用提示词，请先配置：" + promptName);
        }

        List<PromptTemplateSegment> segments = promptTemplateRepository.selectActiveByNameAndVersion(promptName, latestVersion);
        if (segments == null || segments.isEmpty()) {
            throw new IllegalStateException("提示词版本无可用片段：" + promptName + " v" + latestVersion);
        }

        String finalPrompt = promptTemplateMerger.merge(segments);
        if (finalPrompt.isEmpty()) {
            throw new IllegalStateException("提示词内容为空：" + promptName + " v" + latestVersion);
        }

        String redisKey = buildPromptRedisKey(promptName);
        String oldContent = promptCachePort.get(redisKey);
        promptCachePort.set(redisKey, finalPrompt);

        writeLog(segments.get(0).id(), promptName, latestVersion, oldContent, finalPrompt,
                operationType, operator, remark);

        log.info("提示词已同步到 Redis，promptName={} version={} segments={} redisKey={}",
                promptName, latestVersion, segments.size(), redisKey);
        return finalPrompt;
    }

    /**
     * 写入提示词加载日志。
     *
     * @param promptId   提示词主键
     * @param promptName 提示词名称
     * @param version    版本号
     * @param oldContent 旧内容
     * @param newContent 新内容
     */
    private void writeLog(Long promptId, String promptName, Integer version,
                          String oldContent, String newContent,
                          String operationType, String operator, String remark) {
        promptTemplateLogRepository.saveLoadLog(new PromptTemplateLogCommand(
                promptId,
                promptName,
                version,
                oldContent,
                newContent,
                operationType,
                operator,
                remark
        ));
    }

    /**
     * 生成提示词缓存键。
     *
     * @param promptName 提示词名称
     * @return Redis 键
     */
    private String buildPromptRedisKey(String promptName) {
        if (PromptConstant.AGENT_CHAT_ROLE_PROMPT_NAME.equals(promptName)) {
            return PromptConstant.AGENT_CHAT_ROLE_PROMPT_REDIS_KEY;
        }
        return PromptConstant.AGENT_PROMPT_CACHE_PREFIX + promptName;
    }
}
