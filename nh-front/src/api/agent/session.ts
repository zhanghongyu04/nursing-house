import httpInstance from "@/utils/http";

/**
 * 会话类型
 */
export type SessionType = 'chat' | 'pdf';

/**
 * 会话状态
 */
export type SessionStatus = 0 | 1 | 2; // 0-正常，1-已删除，2-已禁用

/**
 * 会话创建DTO
 */
export interface SessionCreateDto {
  sessionType?: SessionType;
  title?: string;
}

/**
 * 会话更新DTO
 */
export interface SessionUpdateDto {
  conversationId: string;
  title?: string;
  status?: SessionStatus;
}

/**
 * 会话VO
 */
export interface SessionVo {
  id: number;
  userId: number;
  conversationId: string;
  title: string;
  sessionType: SessionType;
  status: SessionStatus;
  createTime: string;
  updateTime: string;
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  total: number;
  records: T[];
}

/**
 * 会话管理API
 */
export const sessionAPI = {
  /**
   * 创建新会话
   * @param dto 创建会话请求
   */
  createSession(dto: SessionCreateDto = {}) {
    return httpInstance<SessionVo>({
      url: '/api/v1/agent/session',
      method: 'POST',
      data: dto,
    });
  },

  /**
   * 获取用户会话列表
   * @param sessionType 会话类型（可选）
   */
  getSessionList(sessionType?: SessionType) {
    return httpInstance<SessionVo[]>({
      url: '/api/v1/agent/session/list',
      method: 'GET',
      params: sessionType ? { sessionType } : {},
    });
  },

  /**
   * 分页查询用户会话列表
   * @param page 页码
   * @param pageSize 每页大小
   * @param sessionType 会话类型（可选）
   */
  getSessionPage(page: number = 1, pageSize: number = 10, sessionType?: SessionType) {
    return httpInstance<PageResult<SessionVo>>({
      url: '/api/v1/agent/session/page',
      method: 'GET',
      params: { page, pageSize, sessionType },
    });
  },

  /**
   * 获取会话详情
   * @param conversationId 会话ID
   */
  getSession(conversationId: string) {
    return httpInstance<SessionVo>({
      url: `/api/v1/agent/session/${conversationId}`,
      method: 'GET',
    });
  },

  /**
   * 更新会话信息
   * @param dto 更新会话请求
   */
  updateSession(dto: SessionUpdateDto) {
    return httpInstance<boolean>({
      url: '/api/v1/agent/session',
      method: 'PUT',
      data: dto,
    });
  },

  /**
   * 删除会话
   * @param conversationId 会话ID
   */
  deleteSession(conversationId: string) {
    return httpInstance<boolean>({
      url: `/api/v1/agent/session/${conversationId}`,
      method: 'DELETE',
    });
  },

  /**
   * 更新会话状态
   * @param conversationId 会话ID
   * @param status 新状态
   */
  updateSessionStatus(conversationId: string, status: SessionStatus) {
    return httpInstance<boolean>({
      url: `/api/v1/agent/session/${conversationId}/status`,
      method: 'PATCH',
      params: { status },
    });
  },
};

