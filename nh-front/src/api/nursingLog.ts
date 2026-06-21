import httpInstance from '@/utils/http';

// ========== 机构侧 ==========

export interface NursingLogPageQuery {
  page: number;
  pageSize: number;
  sanaId?: number;
  taskId?: number;
  elderId?: number;
  nurseUserId?: number;
  abnormalFlag?: number;
  content?: string;
  logTimeBegin?: string;
  logTimeEnd?: string;
}

// ========== 护理端 ==========

export interface NursingLogMyPageQuery {
  page: number;
  pageSize: number;
  sanaId?: number;
  taskId?: number;
  elderId?: number;
  abnormalFlag?: number;
  content?: string;
  logTimeBegin?: string;
  logTimeEnd?: string;
}

export interface NursingLogAdd {
  taskId?: number;
  elderId?: number;
  logTime: string;
  content: string;
  abnormalFlag?: number;
  attachmentUrls?: string;
}

export interface NursingLogUpdate {
  taskId?: number;
  elderId?: number;
  logTime?: string;
  content?: string;
  abnormalFlag?: number;
  attachmentUrls?: string;
}

export interface NursingLogExportQuery {
  logIds?: number[];
  sanaId?: number;
  taskId?: number;
  elderId?: number;
  nurseUserId?: number;
  abnormalFlag?: number;
  content?: string;
  logTimeBegin?: string;
  logTimeEnd?: string;
  reportFormat?: 'docx' | 'pdf';
  includeAttachments?: boolean;
}

// ========== 响应行 ==========

export interface NursingLogRow {
  id: number;
  sanaId: number;
  sanaName?: string;
  taskId?: number;
  taskTitle?: string;
  elderId?: number;
  elderName?: string;
  nurseUserId: number;
  nurseUsername?: string;
  logTime: string;
  content: string;
  abnormalFlag: number;
  attachmentUrls?: string;
  createTime?: string;
  updateTime?: string;
}

// ========== 接口 ==========

/** 机构侧：护理日志分页 */
export const getNursingLogPage = (data: NursingLogPageQuery) => {
  return httpInstance({
    url: '/api/v1/nursing-log/page',
    method: 'POST',
    data
  });
};

/** 护理端：我的日志分页 */
export const getMyNursingLogPage = (data: NursingLogMyPageQuery) => {
  return httpInstance({
    url: '/api/v1/nursing-log/my/page',
    method: 'POST',
    data
  });
};

/** 护理端：新增日志 */
export const addNursingLog = (data: NursingLogAdd) => {
  return httpInstance({
    url: '/api/v1/nursing-log/add',
    method: 'POST',
    data
  });
};

/** 护理端：更新日志 */
export const updateNursingLog = (id: number, data: NursingLogUpdate) => {
  return httpInstance({
    url: `/api/v1/nursing-log/update/${id}`,
    method: 'PUT',
    data
  });
};

/** 机构侧：导出护理日志（ZIP） */
export const exportNursingLog = (data: NursingLogExportQuery) => {
  return httpInstance({
    url: '/api/v1/nursing-log/export',
    method: 'POST',
    data,
    responseType: 'blob'
  });
};
