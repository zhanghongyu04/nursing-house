package com.zhiling.agent.application.repository;

import com.zhiling.agent.application.prompt.model.PromptTemplateLogCommand;
import com.zhiling.agent.application.prompt.model.PromptTemplateLogView;
import com.zhiling.agent.application.prompt.model.PromptTemplateChangeLogCommand;

import java.util.List;

/**

 * PromptTemplateLogRepository

 *

 * @author zhanghongyu

 */

public interface PromptTemplateLogRepository {

    void saveLoadLog(PromptTemplateLogCommand command);

    void saveChangeLog(PromptTemplateChangeLogCommand command);

    List<PromptTemplateLogView> listLogs(String promptName, String operationType, int limit);
}

