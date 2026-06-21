package com.zhiling.agent.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiling.model.entity.AgentMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collections;
import java.util.List;

/**
 * Agent 消息明细持久化 Mapper。
 *
 * @author zhanghongyu
 */
@Mapper
public interface AgentMessagePersistenceMapper extends BaseMapper<AgentMessage> {

    @Select("""
            select coalesce(max(seq_no), 0)
            from tb_agent_message
            where user_id = #{userId}
              and conversation_id = #{conversationId}
              and status = 0
            """)
    Integer selectMaxSeqNo(@Param("userId") Long userId, @Param("conversationId") String conversationId);

    default List<AgentMessage> selectByConversation(Long userId, String conversationId) {
        if (userId == null || conversationId == null || conversationId.isBlank()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getUserId, userId)
                .eq(AgentMessage::getConversationId, conversationId)
                .eq(AgentMessage::getStatus, 0)
                .orderByAsc(AgentMessage::getSeqNo)
                .orderByAsc(AgentMessage::getId));
    }

    default List<AgentMessage> selectRecent(Long userId, String conversationId, int limit) {
        if (userId == null || conversationId == null || conversationId.isBlank() || limit <= 0) {
            return Collections.emptyList();
        }
        List<AgentMessage> desc = selectList(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getUserId, userId)
                .eq(AgentMessage::getConversationId, conversationId)
                .eq(AgentMessage::getStatus, 0)
                .orderByDesc(AgentMessage::getSeqNo)
                .orderByDesc(AgentMessage::getId)
                .last("limit " + limit));
        Collections.reverse(desc);
        return desc;
    }

    default int softDeleteByConversation(Long userId, String conversationId) {
        if (userId == null || conversationId == null || conversationId.isBlank()) {
            return 0;
        }
        AgentMessage message = new AgentMessage();
        message.setStatus(1);
        return update(message, new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getUserId, userId)
                .eq(AgentMessage::getConversationId, conversationId)
                .eq(AgentMessage::getStatus, 0));
    }
}
