import httpInstance from '@/utils/http';

// 护理任务模板的调度粒度：按天、周、月生成任务。
export type TaskTemplateScheduleType = 'DAILY' | 'WEEKLY' | 'MONTHLY';
// 执行时间模式：固定时间点或固定间隔。
export type TaskTemplateTimeMode = 'POINT' | 'INTERVAL';

// 列表页查询条件，兼顾标题模糊搜索和启用状态过滤。
export interface TaskTemplatePageQuery {
  page: number;
  pageSize: number;
  taskTitle?: string;
  enabled?: number;
}

// 模板调度配置是任务模板模块的核心结构，前后端共享同一套字段语义。
export interface TaskTemplateScheduleConfig {
  scheduleType: TaskTemplateScheduleType;
  timeMode: TaskTemplateTimeMode;
  weekdays?: number[];
  monthDays?: number[];
  timePoints?: string[];
  startTime?: string;
  endTime?: string;
  intervalMinutes?: number;
}

// 新增和编辑共用同一份表单载荷。
export interface TaskTemplateCreate {
  id?: number;
  sanaId?: number;
  taskTitle: string;
  taskContent?: string;
  taskType?: number;
  priority?: number;
  elderId?: number;
  assigneeUserId?: number;
  timezone?: string;
  scheduleConfig: TaskTemplateScheduleConfig;
  plannedDuration?: number;
  startDate?: string;
  endDate?: string;
  remark?: string;
}

// 任务模板表格行，包含后端回填的展示字段与调度摘要。
export interface TaskTemplateRow {
  id: number;
  sanaId: number;
  sanaName?: string;
  taskTitle: string;
  taskContent?: string;
  taskType: number;
  priority: number;
  elderId?: number;
  elderName?: string;
  assigneeUserId?: number;
  assigneeUsername?: string;
  scheduleType?: TaskTemplateScheduleType;
  timeMode?: TaskTemplateTimeMode;
  scheduleConfig?: TaskTemplateScheduleConfig;
  scheduleDescription?: string;
  timezone?: string;
  plannedDuration: number;
  enabled: number;
  startDate?: string;
  endDate?: string;
  nextExecuteTime?: string;
  remark?: string;
  createTime?: string;
}

// 分页查询护理任务模板。
export const getTaskTemplatePage = (data: TaskTemplatePageQuery) => {
  return httpInstance({
    url: '/api/v1/nursing-task-template/page',
    method: 'POST',
    data,
  });
};

// 创建新的任务模板。
export const createTaskTemplate = (data: TaskTemplateCreate) => {
  return httpInstance({
    url: '/api/v1/nursing-task-template/create',
    method: 'POST',
    data,
  });
};

// 更新已有任务模板。
export const updateTaskTemplate = (data: TaskTemplateCreate) => {
  return httpInstance({
    url: '/api/v1/nursing-task-template/update',
    method: 'PUT',
    data,
  });
};

// 启停模板，控制调度器是否继续生成任务。
export const toggleTaskTemplate = (id: number) => {
  return httpInstance({
    url: `/api/v1/nursing-task-template/toggle/${id}`,
    method: 'POST',
  });
};

// 删除任务模板及其调度配置。
export const removeTaskTemplate = (id: number) => {
  return httpInstance({
    url: `/api/v1/nursing-task-template/remove/${id}`,
    method: 'DELETE',
  });
};

// 按模板手动立即生成一次任务，便于调试和补发。
export const generateFromTemplate = (id: number) => {
  return httpInstance({
    url: `/api/v1/nursing-task-template/generate/${id}`,
    method: 'POST',
  });
};
