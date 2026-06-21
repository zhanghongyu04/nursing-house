import httpInstance from '@/utils/http';

// 老人档案分页查询条件，同时兼容机构筛选与批量导出复用。
export interface ElderPageQuery {
  page: number;
  pageSize: number;
  elderName?: string;
  occupiedBedType?: number;
  roomNumber?: string;
  inpatientsTime?: string;
  familySituation?: number;
  selfCare?: number;
  sanaId?: number;
  elderIds?: number[];
}

// 老人档案主数据结构，兼容列表展示和编辑表单。
export interface ElderRecord {
  id?: number;
  sanaId?: number;
  sanaName?: string;
  elderName?: string;
  sex?: number;
  age?: number;
  idNumber?: string;
  phoneNumber?: string;
  homeAddress?: string;
  familySituation?: number;
  occupiedBedType?: number;
  roomNumber?: string;
  guardianName?: string;
  guardianPhone?: string;
  selfCare?: number;
  inpatientsTime?: string;
  outpatientsTime?: string;
  fees?: number;
  status?: number;
}

export interface ElderAttachmentRecord {
  id?: number;
  elderId?: number;
  fileName?: string;
  fileUrl?: string;
  fileType?: string;
  fileSize?: number;
  attachmentType?: number;
  remark?: string;
  createTime?: string;
}

// 分页查询老人信息。
export const getElderPage = (data: ElderPageQuery) => {
  return httpInstance({
    url: '/api/v1/elder/page',
    method: 'POST',
    data
  });
};

// 新增老人档案。
export const addElder = (data: ElderRecord) => {
  return httpInstance({
    url: '/api/v1/elder/add',
    method: 'POST',
    data
  });
};

// 更新老人档案。
export const updateElder = (data: ElderRecord) => {
  return httpInstance({
    url: '/api/v1/elder/update',
    method: 'PUT',
    data
  });
};

// 删除单个老人档案。
export const deleteElder = (id: number) => {
  return httpInstance({
    url: '/api/v1/elder/delete',
    method: 'DELETE',
    params: { id }
  });
};

// 导出老人 Excel，支持携带当前筛选条件。
export const exportElderExcel = (data: Partial<ElderPageQuery> = {}) => {
  return httpInstance({
    url: '/api/v1/elder/export',
    method: 'POST',
    data,
    responseType: 'blob'
  });
};

// 导入老人 Excel，实际上传由 FormData 承载。
export const importElderExcel = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return httpInstance({
    url: '/api/v1/elder/import',
    method: 'POST',
    data: formData
  });
};

// 查询老人档案图片与附件。
export const getElderAttachments = (elderId: number) => {
  return httpInstance({
    url: '/api/v1/elder/attachments',
    method: 'GET',
    params: { elderId }
  });
};

// 上传老人档案图片或普通附件，attachmentType: 0 图片，1 附件。
export const uploadElderAttachment = (elderId: number, file: File, attachmentType: number, remark?: string) => {
  const formData = new FormData();
  formData.append('elderId', String(elderId));
  formData.append('file', file);
  formData.append('attachmentType', String(attachmentType));
  if (remark) {
    formData.append('remark', remark);
  }
  return httpInstance({
    url: '/api/v1/elder/attachments/upload',
    method: 'POST',
    data: formData
  });
};

// 删除老人档案附件。
export const deleteElderAttachment = (id: number) => {
  return httpInstance({
    url: '/api/v1/elder/attachments/delete',
    method: 'DELETE',
    params: { id }
  });
};

// 机构分页接口在老人页面中复用，用于所属机构下拉选择。
export const getSanatoriumPage = (data: { page: number; pageSize: number; sanaName?: string }) => {
  return httpInstance({
    url: '/api/v1/sanatorium/page',
    method: 'POST',
    data
  });
};

