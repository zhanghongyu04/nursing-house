import httpInstance from "@/utils/http.ts";

// 向量库统计信息接口返回类型
export interface VectorStoreStats {
  totalDocuments: number;
  totalChunks: number;
  collectionSize: number;
  collectionName: string;
}

// 文档信息接口返回类型
export interface DocumentInfo {
  id: string;
  fileName: string;
  fileType: string;
  chunkIndex: number;
  totalChunks: number;
  content: string;
  contentLength: number;
  metadata: Record<string, any>;
}

// 文档列表结果接口返回类型
export interface DocumentListResult {
  documents: DocumentInfo[];
  total: number;
  page: number;
  pageSize: number;
}

// API 响应类型
interface ApiResponse<T> {
  code: number;
  data: T;
  message: string;
}

/**
 * 获取向量库统计信息
 */
export const getVectorStoreStatsAPI = (): Promise<ApiResponse<VectorStoreStats>> => {
  return httpInstance({
    url: "/api/v1/vector-store/stats",
    method: "GET"
  });
};

/**
 * 获取文档列表（分页）
 */
export const getDocumentsAPI = (page: number = 1, pageSize: number = 10): Promise<ApiResponse<DocumentListResult>> => {
  return httpInstance({
    url: "/api/v1/vector-store/documents",
    method: "GET",
    params: { page, pageSize }
  });
};

/**
 * 根据文件名获取文档
 */
export const getDocumentsByFileNameAPI = (fileName: string): Promise<ApiResponse<DocumentInfo[]>> => {
  return httpInstance({
    url: "/api/v1/vector-store/documents/by-filename",
    method: "GET",
    params: { fileName }
  });
};

/**
 * 删除指定文件的所有文档
 */
export const deleteDocumentsByFileNameAPI = (fileName: string): Promise<ApiResponse<number>> => {
  return httpInstance({
    url: "/api/v1/vector-store/documents/by-filename",
    method: "DELETE",
    params: { fileName }
  });
};

/**
 * 根据文档ID删除文档
 */
export const deleteDocumentAPI = (documentId: string): Promise<ApiResponse<boolean>> => {
  return httpInstance({
    url: `/api/v1/vector-store/documents/${documentId}`,
    method: "DELETE"
  });
};

/**
 * 批量删除文档
 */
export const deleteDocumentsBatchAPI = (documentIds: string[]): Promise<ApiResponse<number>> => {
  return httpInstance({
    url: "/api/v1/vector-store/documents/batch",
    method: "DELETE",
    data: documentIds
  });
};

/**
 * 清空向量库
 */
export const clearVectorStoreAPI = (): Promise<ApiResponse<number>> => {
  return httpInstance({
    url: "/api/v1/vector-store/clear",
    method: "DELETE"
  });
};

/**
 * 上传单个文档到向量库
 */
export const uploadDocumentAPI = (file: File, sanaId?: number): Promise<ApiResponse<string>> => {
  const formData = new FormData();
  formData.append('file', file);
  if (typeof sanaId === 'number') {
    formData.append('sanaId', String(sanaId));
  }
  return httpInstance({
    url: "/api/v1/vector-store/upload",
    method: "POST",
    data: formData,
    timeout: 300000
  });
};

/**
 * 批量上传文档到向量库
 */
export const uploadDocumentsBatchAPI = (files: File[], sanaId?: number): Promise<ApiResponse<string>> => {
  const formData = new FormData();
  files.forEach(file => {
    formData.append('files', file);
  });
  if (typeof sanaId === 'number') {
    formData.append('sanaId', String(sanaId));
  }
  return httpInstance({
    url: "/api/v1/vector-store/upload/batch",
    method: "POST",
    data: formData,
    timeout: 300000
  });
};

/**
 * 获取支持的文件类型
 */
export interface SupportedFileTypes {
  fileExtensions: string[];
  mimeTypes: string[];
}

export const getSupportedFileTypesAPI = (): Promise<ApiResponse<SupportedFileTypes>> => {
  return httpInstance({
    url: "/api/v1/vector-store/supported-types",
    method: "GET"
  });
};

