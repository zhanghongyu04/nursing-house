package com.zhiling.agent.interfaces.http;

import com.zhiling.common.result.PageResult;
import com.zhiling.common.result.Result;
import com.zhiling.framework.llm.model.AgentHistoryView;
import com.zhiling.framework.llm.service.AgentHistoryViewService;
import com.zhiling.framework.llm.model.AgentSessionCreateDto;
import com.zhiling.framework.llm.model.AgentSessionUpdateDto;
import com.zhiling.framework.llm.model.AgentSessionVo;
import com.zhiling.framework.llm.service.AgentChatFlowService;
import com.zhiling.framework.llm.service.AgentSessionService;
import com.zhiling.framework.llm.service.AgentSessionHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;


@RestController
@RequestMapping("/api/v1/agent")
@Tag(name = "智能体接口")
@Slf4j
/**
 * AgentController
 *
 * @author zhanghongyu
 */
public class AgentController {
    private final AgentSessionService agentSessionService;
    private final AgentSessionHistoryService agentSessionHistoryService;
    private final AgentHistoryViewService agentHistoryViewService;
    private final AgentChatFlowService agentChatFlowService;

    public AgentController(AgentSessionService agentSessionService,
                           AgentSessionHistoryService agentSessionHistoryService,
                           AgentHistoryViewService agentHistoryViewService,
                           AgentChatFlowService agentChatFlowService) {
        this.agentSessionService = agentSessionService;
        this.agentSessionHistoryService = agentSessionHistoryService;
        this.agentHistoryViewService = agentHistoryViewService;
        this.agentChatFlowService = agentChatFlowService;
    }

    @PostMapping(value = "/sence", produces = "text/html;charset=utf-8")
    @Operation(summary = "智能体对话")
    public Flux<String> chat(@RequestParam(value = "prompt", defaultValue = "") String prompt,
                             @RequestParam(required = false) String chatId,
                             @RequestParam(value = "files", required = false) List<MultipartFile> files,
                             HttpServletResponse response) {
        return agentChatFlowService.chat(prompt, chatId, files, response);
    }

    /**
     * 方法：PathVariable
     *
     * @author zhanghongyu
     */
    @GetMapping("/{type}")
    @Operation(summary = "获取会话ID列表")
    public List<String> getChatIds(@PathVariable("type") String type) {
        return agentSessionHistoryService.getChatIds(type);
    }

    /**
     * 方法：PathVariable
     *
     * @author zhanghongyu
     */
    @GetMapping("/{type}/{chatId}")
    @Operation(summary = "获取会话记录")
    public List<AgentHistoryView> getChatHistory(@PathVariable("type") String type, @PathVariable("chatId") String chatId) {
        return agentHistoryViewService.getChatHistoryView(type, chatId);
    }

    /**
     * 方法：createSession
     *
     * @author zhanghongyu
     */
    @PostMapping("/session")
    @Operation(summary = "创建新会话")
    public Result<AgentSessionVo> createSession(@RequestBody AgentSessionCreateDto dto) {
        return Result.success(agentSessionService.createSession(dto));
    }

    @GetMapping("/session/list")
    @Operation(summary = "获取用户会话列表")
    public Result<java.util.List<AgentSessionVo>> getUserSessions(
            @RequestParam(required = false) String sessionType) {
        return Result.success(agentSessionService.getUserSessions(sessionType));
    }

    @GetMapping("/session/page")
    @Operation(summary = "分页查询用户会话列表")
    public Result<PageResult> getUserSessionsPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String sessionType) {
        return Result.success(agentSessionService.getUserSessionsPage(page, pageSize, sessionType));
    }

    /**
     * 方法：getSession
     *
     * @author zhanghongyu
     */
    @GetMapping("/session/{conversationId}")
    @Operation(summary = "获取会话详情")
    public Result<AgentSessionVo> getSession(@PathVariable String conversationId) {
        AgentSessionVo sessionVo = agentSessionService.getSessionByConversationId(conversationId);
        if (sessionVo == null) {
            return Result.fail("会话不存在");
        }
        return Result.success(sessionVo);
    }

    /**
     * 方法：updateSession
     *
     * @author zhanghongyu
     */
    @PutMapping("/session")
    @Operation(summary = "更新会话信息")
    public Result<Boolean> updateSession(@RequestBody AgentSessionUpdateDto dto) {
        return Result.success(agentSessionService.updateSession(dto));
    }

    /**
     * 方法：deleteSession
     *
     * @author zhanghongyu
     */
    @DeleteMapping("/session/{conversationId}")
    @Operation(summary = "删除会话")
    public Result<Boolean> deleteSession(@PathVariable String conversationId) {
        return Result.success(agentSessionService.deleteSession(conversationId));
    }

    @PatchMapping("/session/{conversationId}/status")
    @Operation(summary = "更新会话状态")
    public Result<Boolean> updateSessionStatus(
            @PathVariable String conversationId,
            @RequestParam Integer status) {
        return Result.success(agentSessionService.updateSessionStatus(conversationId, status));
    }
}



