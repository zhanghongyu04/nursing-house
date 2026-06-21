package com.zhiling.agent.application;

import java.util.Optional;

/**
 * 智能问答中的内部数据问题直连服务。
 *
 * @author zhanghongyu
 */
public interface AgentInternalDataQueryService {

    /**
     * 尝试处理内部数据查询问题。
     *
     * @param prompt 用户提问
     * @return 命中则返回答案；未命中则返回 empty
     */
    Optional<String> tryHandle(String prompt);
}