import httpInstance from '@/utils/http';

// ========== 机构侧 ==========

export interface NursingTaskPageQuery {
  page: number;
  pageSize: number;
  sanaId?: number;
  elderId?: number;
  assigneeUserId?: number;
  taskTitle?: string;
  taskType?: number;
  priority?: number;
  status?: number;
  plannedStartBegin?: string;
  plannedStartEnd?: string;
}

export interface NursingTaskDispatch {
  sanaId?: number;
  elderId?: number;
  taskTitle: string;
  taskContent?: string;
  taskType: number;
  priority: number;
  assigneeUserId: number;
  plannedStartTime?: string;
  plannedEndTime?: string;
  remark?: string;
}

export interface NursingTaskUpdate {
  id: number;
  elderId?: number;
  assigneeUserId?: number;
  taskTitle?: string;
  taskContent?: string;
  taskType?: number;
  priority?: number;
  status?: number;
  plannedStartTime?: string;
  plannedEndTime?: string;
  remark?: string;
}

// ========== 护理端 ==========

export interface NursingTaskMyPageQuery {
  page: number;
  pageSize: number;
  sanaId?: number;
  elderId?: number;
  taskTitle?: string;
  taskType?: number;
  priority?: number;
  status?: number;
  plannedStartBegin?: string;
  plannedStartEnd?: string;
}

// ========== 响应行 ==========

export interface NursingTaskRow {
  id: number;
  sanaId: number;
  sanaName?: string;
  elderId?: number;
  elderName?: string;
  taskTitle: string;
  taskContent?: string;
  taskType: number;
  priority: number;
  assigneeUserId: number;
  assigneeUsername?: string;
  assignerUserId?: number;
  assignerUsername?: string;
  status: number;
  plannedStartTime?: string;
  plannedEndTime?: string;
  completionTime?: string;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

// ========== 接口 ==========

/** 机构侧：护理任务分页 */
export const getNursingTaskPage = (data: NursingTaskPageQuery) => {
  return httpInstance({
    url: '/api/v1/nursing-task/page',
    method: 'POST',
    data
  });
};

/** 机构侧：下发护理任务 */
export const dispatchNursingTask = (data: NursingTaskDispatch) => {
  return httpInstance({
    url: '/api/v1/nursing-task/dispatch',
    method: 'POST',
    data
  });
};

/** 机构侧：更新护理任务 */
export const updateNursingTask = (data: NursingTaskUpdate) => {
  return httpInstance({
    url: '/api/v1/nursing-task/update',
    method: 'PUT',
    data
  });
};

/** 机构侧：取消护理任务 */
export const cancelNursingTask = (id: number) => {
  return httpInstance({
    url: `/api/v1/nursing-task/cancel/${id}`,
    method: 'POST'
  });
};

export const reactivateNursingTask = (id: number) => {
  return httpInstance({
    url: `/api/v1/nursing-task/reactivate/${id}`,
    method: 'POST'
  });
};

export const removeNursingTask = (id: number) => {
  return httpInstance({
    url: `/api/v1/nursing-task/remove/${id}`,
    method: 'DELETE'
  });
};

/** 护理端：我的任务分页 */
export const getMyNursingTaskPage = (data: NursingTaskMyPageQuery) => {
  return httpInstance({
    url: '/api/v1/nursing-task/my/page',
    method: 'POST',
    data
  });
};

/** 护理端：完成任务 */
export const completeNursingTask = (id: number) => {
  return httpInstance({
    url: `/api/v1/nursing-task/complete/${id}`,
    method: 'POST'
  });
};
