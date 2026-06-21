import httpInstance from "@/utils/http.ts";

interface ApiResponse<T> {
  code: number;
  data: T;
  message?: string;
}

export interface PromptTemplateSummary {
  promptName: string;
  version: number;
  status: number;
  segmentCount: number;
  updateTime: string;
}

export interface PromptTemplateDetail {
  id: number;
  promptName: string;
  promptIndex: number;
  promptContent: string;
  version: number;
  status: number;
  createTime: string;
  updateTime: string;
}

export interface PromptTemplateActiveView {
  promptName: string;
  version: number;
  segments: PromptTemplateDetail[];
  mergedContent: string;
}

export interface PromptTemplateCacheView {
  promptName: string;
  redisKey: string;
  content: string;
}

export interface PromptTemplateLogView {
  id: number;
  promptId: number;
  promptName: string;
  promptIndex: number;
  oldVersion: number | null;
  newVersion: number | null;
  oldContent: string | null;
  newContent: string | null;
  operationType: string;
  operator: string;
  remark: string;
  createTime: string;
}

export interface PromptSupportedTypeView {
  promptName: string;
  label: string;
  description: string;
  exists: boolean;
  maxVersion: number | null;
  activeVersion: number | null;
}

export const listPromptTemplatesAPI = (promptName?: string): Promise<ApiResponse<PromptTemplateSummary[]>> => {
  return httpInstance({
    url: "/api/v1/agent/prompt/templates",
    method: "GET",
    params: { promptName }
  });
};

export const listSupportedPromptTypesAPI = (): Promise<ApiResponse<PromptSupportedTypeView[]>> => {
  return httpInstance({
    url: "/api/v1/agent/prompt/supported-types",
    method: "GET"
  });
};

export const getActivePromptAPI = (promptName: string): Promise<ApiResponse<PromptTemplateActiveView>> => {
  return httpInstance({
    url: `/api/v1/agent/prompt/templates/${encodeURIComponent(promptName)}/active`,
    method: "GET"
  });
};

export const getPromptVersionAPI = (promptName: string, version: number): Promise<ApiResponse<PromptTemplateActiveView>> => {
  return httpInstance({
    url: `/api/v1/agent/prompt/templates/${encodeURIComponent(promptName)}/versions/${version}`,
    method: "GET"
  });
};

export const previewPromptAPI = (data: {
  promptName: string;
  version?: number;
  segments?: string[];
}): Promise<ApiResponse<{ promptName: string; version: number | null; mergedContent: string }>> => {
  return httpInstance({
    url: "/api/v1/agent/prompt/preview",
    method: "POST",
    data
  });
};

export const getPromptCacheAPI = (promptName: string): Promise<ApiResponse<PromptTemplateCacheView>> => {
  return httpInstance({
    url: `/api/v1/agent/prompt/templates/${encodeURIComponent(promptName)}/cache`,
    method: "GET"
  });
};

export const syncPromptAPI = (promptName: string): Promise<ApiResponse<{ promptName: string; redisKey: string; content: string }>> => {
  return httpInstance({
    url: "/api/v1/agent/prompt/sync",
    method: "POST",
    data: { promptName }
  });
};

export const createPromptVersionAPI = (promptName: string, data: {
  segments?: Array<{ promptIndex: number; promptContent: string }>;
  remark?: string;
  syncToRedis?: boolean;
}): Promise<ApiResponse<PromptTemplateActiveView>> => {
  return httpInstance({
    url: `/api/v1/agent/prompt/templates/${encodeURIComponent(promptName)}/versions`,
    method: "POST",
    data
  });
};

export const createPromptSegmentAPI = (promptName: string, version: number, data: {
  promptIndex?: number;
  promptContent: string;
  remark?: string;
  syncToRedis?: boolean;
}): Promise<ApiResponse<PromptTemplateActiveView>> => {
  return httpInstance({
    url: `/api/v1/agent/prompt/templates/${encodeURIComponent(promptName)}/versions/${version}/segments`,
    method: "POST",
    data
  });
};

export const activatePromptVersionAPI = (promptName: string, version: number, data?: {
  remark?: string;
  syncToRedis?: boolean;
}): Promise<ApiResponse<PromptTemplateActiveView>> => {
  return httpInstance({
    url: `/api/v1/agent/prompt/templates/${encodeURIComponent(promptName)}/versions/${version}/activate`,
    method: "PUT",
    data
  });
};

export const disablePromptVersionAPI = (promptName: string, version: number, data?: {
  remark?: string;
  syncToRedis?: boolean;
}): Promise<ApiResponse<PromptTemplateActiveView>> => {
  return httpInstance({
    url: `/api/v1/agent/prompt/templates/${encodeURIComponent(promptName)}/versions/${version}/disable`,
    method: "PUT",
    data
  });
};

export const updatePromptSegmentAPI = (id: number, data: {
  promptContent: string;
  remark?: string;
  syncToRedis?: boolean;
}): Promise<ApiResponse<PromptTemplateActiveView>> => {
  return httpInstance({
    url: `/api/v1/agent/prompt/templates/segments/${id}`,
    method: "PUT",
    data
  });
};

export const enablePromptSegmentAPI = (id: number, data?: {
  remark?: string;
  syncToRedis?: boolean;
}): Promise<ApiResponse<PromptTemplateActiveView>> => {
  return httpInstance({
    url: `/api/v1/agent/prompt/templates/segments/${id}/enable`,
    method: "PUT",
    data
  });
};

export const disablePromptSegmentAPI = (id: number, data?: {
  remark?: string;
  syncToRedis?: boolean;
}): Promise<ApiResponse<PromptTemplateActiveView>> => {
  return httpInstance({
    url: `/api/v1/agent/prompt/templates/segments/${id}/disable`,
    method: "PUT",
    data
  });
};

export const listPromptLogsAPI = (params: {
  promptName?: string;
  operationType?: string;
  limit?: number;
}): Promise<ApiResponse<PromptTemplateLogView[]>> => {
  return httpInstance({
    url: "/api/v1/agent/prompt/logs",
    method: "GET",
    params
  });
};
