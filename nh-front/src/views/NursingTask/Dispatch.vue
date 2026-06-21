<template>
  <div class="nursing-task-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">护理任务管理</h2>
        <p class="page-desc">下发、管理本机构的护理任务</p>
      </div>
      <el-button v-if="canDispatchTask" type="primary" @click="openDispatchDialog">下发任务</el-button>
    </div>

    <!-- 筛选 -->
    <el-card shadow="never" class="query-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="任务标题">
          <el-input v-model.trim="queryForm.taskTitle" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="任务类型">
          <el-select v-model="queryForm.taskType" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="opt in dictOptions.taskType" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="queryForm.priority" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="opt in dictOptions.priority" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="opt in dictOptions.status" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="taskTitle" label="任务标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="taskType" label="任务类型" width="110" align="center">
          <template #default="{ row }">{{ dictLabel(dictOptions.taskType, row.taskType) }}</template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" size="small">{{ dictLabel(dictOptions.priority, row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ dictLabel(dictOptions.status, row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="elderName" label="关联老人" width="100" show-overflow-tooltip />
        <el-table-column prop="assigneeUsername" label="执行人" width="100" show-overflow-tooltip />
        <el-table-column prop="plannedStartTime" label="计划开始" width="170" />
        <el-table-column prop="plannedEndTime" label="计划结束" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="canUpdateTask" link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button v-if="canCancelTask && (row.status === 0 || row.status === 1 || row.status === 4)" link type="danger" size="small" @click="handleCancel(row)">取消</el-button>
            <el-button v-if="canReactivateTask && row.status === 3" link type="primary" size="small" @click="handleReactivate(row)">激活</el-button>
            <el-button v-if="canRemoveTask && (row.status === 0 || row.status === 3 || row.status === 4)" link type="danger" size="small" @click="handleRemove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageQuery.page"
          v-model:page-size="pageQuery.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <NursingTaskDetailDialog
      v-model="detailVisible"
      :task="detailRow"
      :dict-options="dictOptions"
      :priority-tag-type="priorityTagType"
      :status-tag-type="statusTagType"
    />

    <!-- 下发/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '下发任务' : '编辑任务'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item v-if="isMultiScope" label="目标机构" prop="targetSanaId">
          <el-select v-model="selectedSanaId" placeholder="请选择目标机构" filterable :disabled="dialogMode === 'edit'" style="width: 100%" @change="handleOrgChange">
            <el-option v-for="item in sanatoriumOptions" :key="item.id" :label="item.sanaName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务标题" prop="taskTitle">
          <el-input v-model.trim="formData.taskTitle" maxlength="200" />
        </el-form-item>
        <el-form-item label="任务内容" prop="taskContent">
          <el-input v-model="formData.taskContent" type="textarea" :rows="3" maxlength="2000" />
        </el-form-item>
        <el-form-item label="关联老人">
          <el-select v-model="formData.elderId" placeholder="请选择老人" clearable filterable style="width: 100%">
            <el-option v-for="e in elderOptions" :key="e.id" :label="e.displayLabel" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务类型" prop="taskType">
          <el-select v-model="formData.taskType" style="width: 100%">
            <el-option v-for="opt in dictOptions.taskType" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="formData.priority" style="width: 100%">
            <el-option v-for="opt in dictOptions.priority" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行人" prop="assigneeUserId">
          <el-select v-model="formData.assigneeUserId" placeholder="请选择执行人" clearable filterable style="width: 100%">
            <el-option v-for="u in nurseOptions" :key="u.id" :label="u.username" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划时间">
          <el-date-picker
            v-model="plannedTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :default-time="defaultTimeRange"
            :disabled-date="disablePastDate"
            :shortcuts="dateShortcuts"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted, onBeforeUnmount } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getNursingTaskPage,
  dispatchNursingTask,
  updateNursingTask,
  cancelNursingTask,
  reactivateNursingTask,
  removeNursingTask,
  type NursingTaskRow,
  type NursingTaskDispatch,
} from '@/api/nursingTask';
import { getElderPage } from '@/api/elder';
import { getUserPageAPI } from '@/api/admin';
import { getDictItemsBatch } from '@/api/dict';
import { getSanatoriumPage } from '@/api/sanatorium';
import { useUserStore } from '@/stores/userStore';
import { hasResourcePath } from '@/constants/authRoles';
import { normalizeDateTimeText } from '@/utils/dateTime';
import NursingTaskDetailDialog from '@/components/NursingTaskDetailDialog.vue';

interface DictOption { value: number; label: string }
interface StringOption { value: string; label: string; sortNo?: number; desc?: string }
interface ElderOption { id: number; elderName: string; displayLabel: string }
interface NurseOption { id: number; username: string }

const buildElderLabel = (r: any): string => {
  const parts: string[] = [];
  if (r.sex != null) parts.push(r.sex === 1 ? '男' : '女');
  if (r.age != null) parts.push(`${r.age}岁`);
  return parts.length > 0 ? `${r.elderName || ''}（${parts.join(' / ')}）` : (r.elderName || '');
};

const toNumberOption = (item: any): DictOption | null => {
  const numericValue = Number(item?.itemValue);
  if (!Number.isFinite(numericValue)) return null;
  return { value: numericValue, label: String(item?.itemLabel || '') };
};

const toStringOption = (item: any): StringOption | null => {
  const value = String(item?.itemValue || '').trim();
  const label = String(item?.itemLabel || '').trim();
  if (!value || !label) return null;
  const sortNo = Number(item?.sortNo);
  const desc = String(item?.itemDesc || item?.item_desc || '').trim();
  return { value, label, sortNo: Number.isFinite(sortNo) ? sortNo : 0, desc };
};

const loading = ref(false);
const tableData = ref<NursingTaskRow[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const detailRow = ref<NursingTaskRow | null>(null);
const autoRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null);

const queryForm = reactive({
  taskTitle: '',
  taskType: undefined as number | undefined,
  priority: undefined as number | undefined,
  status: undefined as number | undefined,
});

const pageQuery = reactive({ page: 1, pageSize: 10 });

const dictOptions = reactive<Record<string, DictOption[]>>({
  status: [],
  priority: [],
  taskType: [],
});
const timeShortcutOptions = ref<StringOption[]>([]);
const defaultTimeOptions = ref<StringOption[]>([]);
const tagStyleOptions = ref<StringOption[]>([]);
const nurseRoleId = ref(4);

const elderOptions = ref<ElderOption[]>([]);
const nurseOptions = ref<NurseOption[]>([]);

const userStore = useUserStore();
const canDispatchTask = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-task/dispatch'));
const canUpdateTask = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-task/update'));
const canCancelTask = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-task/cancel/**'));
const canReactivateTask = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-task/reactivate/**'));
const canRemoveTask = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-task/remove/**'));
const isMultiScope = computed(() => (userStore.userInfo.sanaScopeIds?.length ?? 0) > 1);
const selectedSanaId = ref<number | undefined>(undefined);
const sanatoriumOptions = ref<{ id: number; sanaName: string }[]>([]);
const effectiveSanaId = computed(() => {
  if (!isMultiScope.value) return userStore.userInfo.sanaId ?? userStore.userInfo.sanaScopeIds?.[0];
  return selectedSanaId.value;
});

const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'edit'>('create');
const submitting = ref(false);
const formRef = ref<FormInstance>();
const plannedTimeRange = ref<string[]>([]);

const defaultTimeRange = ref<[Date, Date]>([
  new Date(2000, 0, 1, 8, 0, 0),
  new Date(2000, 0, 1, 18, 0, 0),
]);

const disablePastDate = (date: Date) => date.getTime() < Date.now() - 86400000;

const parseHms = (value: string, fallback: { h: number; m: number; s: number }) => {
  const parts = value.split(':').map((p) => Number(p));
  if (parts.length !== 3 || parts.some((n) => !Number.isFinite(n))) return fallback;
  return { h: parts[0], m: parts[1], s: parts[2] };
};

const resolveDefaultTime = (key: 'START' | 'END', fallback: string) => {
  const found = defaultTimeOptions.value.find((o) => o.value === key);
  return found?.label || fallback;
};

const getTimeWindow = () => {
  const start = parseHms(resolveDefaultTime('START', '08:00:00'), { h: 8, m: 0, s: 0 });
  const end = parseHms(resolveDefaultTime('END', '18:00:00'), { h: 18, m: 0, s: 0 });
  return { start, end };
};

const buildShortcutRange = (key: string): [Date, Date] | null => {
  const { start, end } = getTimeWindow();
  const now = new Date();
  if (key === 'TODAY') {
    const s = new Date(); s.setHours(start.h, start.m, start.s, 0);
    const e = new Date(); e.setHours(end.h, end.m, end.s, 0);
    return [s, e];
  }
  if (key === 'THIS_WEEK') {
    const day = now.getDay() || 7;
    const s = new Date(now); s.setDate(now.getDate() - day + 1); s.setHours(start.h, start.m, start.s, 0);
    const e = new Date(now); e.setDate(now.getDate() - day + 7); e.setHours(end.h, end.m, end.s, 0);
    return [s, e];
  }
  if (key === 'NEXT_WEEK') {
    const day = now.getDay() || 7;
    const s = new Date(now); s.setDate(now.getDate() - day + 8); s.setHours(start.h, start.m, start.s, 0);
    const e = new Date(s); e.setDate(s.getDate() + 6); e.setHours(end.h, end.m, end.s, 0);
    return [s, e];
  }
  return null;
};

const dateShortcuts = computed(() => {
  const source = timeShortcutOptions.value.filter((o) => ['TODAY', 'THIS_WEEK', 'NEXT_WEEK'].includes(o.value));
  return source.map((item) => ({
    text: item.label,
    value: () => buildShortcutRange(item.value) || [new Date(), new Date()],
  }));
});

const defaultForm = (): NursingTaskDispatch & { elderId?: number } => ({
  taskTitle: '',
  taskContent: '',
  elderId: undefined as unknown as number,
  taskType: 0,
  priority: 2,
  assigneeUserId: undefined as unknown as number,
  plannedStartTime: '',
  plannedEndTime: '',
  remark: '',
});

const formData = reactive<NursingTaskDispatch & { id?: number; elderId?: number }>(defaultForm());

const formRules: FormRules = {
  targetSanaId: [{
    validator: (_rule: any, _value: any, callback: any) => {
      if (isMultiScope.value && !selectedSanaId.value) {
        callback(new Error('请选择目标机构'));
        return;
      }
      callback();
    },
    trigger: 'change',
  }],
  taskTitle: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  assigneeUserId: [{ required: true, message: '请选择执行人', trigger: 'change' }],
};

const dictLabel = (opts: DictOption[], val: number) => opts.find(o => o.value === val)?.label ?? '-';

const getTagType = (scope: 'PRIORITY' | 'STATUS', value: number, fallback: string) => {
  const key = `${scope}_${value}`;
  const target = tagStyleOptions.value.find((o) => o.value === key);
  return (target?.label || fallback) as any;
};

const priorityTagType = (val: number) => {
  if (val >= 4) return getTagType('PRIORITY', 4, 'danger');
  if (val === 3) return getTagType('PRIORITY', 3, 'warning');
  if (val === 2) return getTagType('PRIORITY', 2, '');
  return getTagType('PRIORITY', 1, 'info');
};

const statusTagType = (val: number) => {
  if (val === 0) return getTagType('STATUS', 0, 'info');
  if (val === 1) return getTagType('STATUS', 1, 'warning');
  if (val === 2) return getTagType('STATUS', 2, 'success');
  if (val === 3) return getTagType('STATUS', 3, 'danger');
  if (val === 4) return getTagType('STATUS', 4, 'danger');
  return getTagType('STATUS', 0, 'info');
};

const loadDictOptions = async () => {
  try {
    const res: any = await getDictItemsBatch([
      'NURSING_TASK_STATUS',
      'NURSING_TASK_PRIORITY',
      'NURSING_TASK_TYPE',
      'NURSING_TASK_TIME_SHORTCUT',
      'NURSING_TASK_DEFAULT_TIME',
      'NURSING_TASK_TAG_STYLE',
      'USER_ROLE_ID_MAP',
    ]);
    const map = res?.data || {};
    dictOptions.status = Array.isArray(map.NURSING_TASK_STATUS) ? map.NURSING_TASK_STATUS.map(toNumberOption).filter(Boolean) as DictOption[] : [];
    dictOptions.priority = Array.isArray(map.NURSING_TASK_PRIORITY) ? map.NURSING_TASK_PRIORITY.map(toNumberOption).filter(Boolean) as DictOption[] : [];
    dictOptions.taskType = Array.isArray(map.NURSING_TASK_TYPE) ? map.NURSING_TASK_TYPE.map(toNumberOption).filter(Boolean) as DictOption[] : [];
    timeShortcutOptions.value = Array.isArray(map.NURSING_TASK_TIME_SHORTCUT)
      ? (map.NURSING_TASK_TIME_SHORTCUT.map(toStringOption).filter(Boolean) as StringOption[]).sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0))
      : [];
    defaultTimeOptions.value = Array.isArray(map.NURSING_TASK_DEFAULT_TIME)
      ? (map.NURSING_TASK_DEFAULT_TIME.map(toStringOption).filter(Boolean) as StringOption[]).sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0))
      : [];
    const start = parseHms(defaultTimeOptions.value.find((o) => o.value === 'START')?.label || '08:00:00', { h: 8, m: 0, s: 0 });
    const end = parseHms(defaultTimeOptions.value.find((o) => o.value === 'END')?.label || '18:00:00', { h: 18, m: 0, s: 0 });
    defaultTimeRange.value = [
      new Date(2000, 0, 1, start.h, start.m, start.s),
      new Date(2000, 0, 1, end.h, end.m, end.s),
    ];
    tagStyleOptions.value = Array.isArray(map.NURSING_TASK_TAG_STYLE)
      ? (map.NURSING_TASK_TAG_STYLE.map(toStringOption).filter(Boolean) as StringOption[]).sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0))
      : [];
    const roleOptions = Array.isArray(map.USER_ROLE_ID_MAP) ? map.USER_ROLE_ID_MAP : [];
    const nurse = roleOptions.find((o: any) => String(o?.itemLabel || '').trim() === 'NURSE');
    const parsedRoleId = Number(nurse?.itemValue);
    if (Number.isFinite(parsedRoleId)) nurseRoleId.value = parsedRoleId;
  } catch { /* ignore */ }
};

const loadElderOptions = async () => {
  const targetSanaId = effectiveSanaId.value;
  if (isMultiScope.value && !targetSanaId) {
    elderOptions.value = [];
    return;
  }
  try {
    const params: any = { page: 1, pageSize: 999 };
    if (targetSanaId) params.sanaId = targetSanaId;
    const res: any = await getElderPage(params);
    if (res?.code === 200 && res?.data?.records) {
      elderOptions.value = res.data.records.map((r: any) => ({ id: r.id, elderName: r.elderName, displayLabel: buildElderLabel(r) }));
    }
  } catch { /* ignore */ }
};

const loadNurseOptions = async () => {
  const targetSanaId = effectiveSanaId.value;
  if (isMultiScope.value && !targetSanaId) {
    nurseOptions.value = [];
    return;
  }
  try {
    const params: any = { page: 1, pageSize: 999, roleId: nurseRoleId.value };
    if (targetSanaId) params.sanaId = targetSanaId;
    const res: any = await getUserPageAPI(params);
    if (res?.code === 200 && res?.data?.records) {
      nurseOptions.value = res.data.records
        .map((r: any) => ({ id: r.id, username: r.username }));
    }
  } catch { /* ignore */ }
};

const loadSanatoriumOptions = async () => {
  if (!isMultiScope.value) return;
  try {
    const pageSize = 200;
    let page = 1;
    let totalCount = 0;
    const list: { id: number; sanaName: string }[] = [];
    do {
      const res: any = await getSanatoriumPage({ page, pageSize });
      const records = res?.data?.records || [];
      totalCount = Number(res?.data?.total || 0);
      records.forEach((item: any) => {
        if (typeof item.id === 'number') {
          list.push({ id: item.id, sanaName: item.sanaName || `机构#${item.id}` });
        }
      });
      page += 1;
    } while (list.length < totalCount && totalCount > 0);
    sanatoriumOptions.value = list;
  } catch { /* ignore */ }
};

const handleOrgChange = () => {
  formData.assigneeUserId = undefined as unknown as number;
  formData.elderId = undefined as unknown as number;
  loadNurseOptions();
  loadElderOptions();
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res: any = await getNursingTaskPage({
      ...pageQuery,
      ...queryForm,
    });
    if (res?.code === 200 && res?.data) {
      tableData.value = res.data.records || [];
      total.value = res.data.total || 0;
    }
  } catch { /* ignore */ }
  loading.value = false;
};

const startAutoRefresh = () => {
  if (autoRefreshTimer.value) return;
  autoRefreshTimer.value = setInterval(() => {
    fetchData();
  }, 30 * 1000);
};

const stopAutoRefresh = () => {
  if (!autoRefreshTimer.value) return;
  clearInterval(autoRefreshTimer.value);
  autoRefreshTimer.value = null;
};

const handleSearch = () => {
  pageQuery.page = 1;
  fetchData();
};

const handleReset = () => {
  queryForm.taskTitle = '';
  queryForm.taskType = undefined;
  queryForm.priority = undefined;
  queryForm.status = undefined;
  pageQuery.page = 1;
  fetchData();
};

const viewDetail = (row: NursingTaskRow) => {
  detailRow.value = row;
  detailVisible.value = true;
};

const openDispatchDialog = () => {
  if (!canDispatchTask.value) {
    ElMessage.warning('暂无下发任务权限');
    return;
  }
  dialogMode.value = 'create';
  selectedSanaId.value = undefined;
  Object.assign(formData, defaultForm());
  plannedTimeRange.value = [];
  if (!isMultiScope.value) {
    loadNurseOptions();
    loadElderOptions();
  } else {
    nurseOptions.value = [];
    elderOptions.value = [];
  }
  dialogVisible.value = true;
};

const openEditDialog = (row: NursingTaskRow) => {
  if (!canUpdateTask.value) {
    ElMessage.warning('暂无编辑任务权限');
    return;
  }
  dialogMode.value = 'edit';
  selectedSanaId.value = row.sanaId;
  const normalizedPlannedStartTime = normalizeDateTimeText(row.plannedStartTime);
  const normalizedPlannedEndTime = normalizeDateTimeText(row.plannedEndTime);
  Object.assign(formData, {
    id: row.id,
    taskTitle: row.taskTitle,
    taskContent: row.taskContent || '',
    elderId: row.elderId,
    taskType: row.taskType,
    priority: row.priority,
    assigneeUserId: row.assigneeUserId,
    plannedStartTime: normalizedPlannedStartTime,
    plannedEndTime: normalizedPlannedEndTime,
    remark: row.remark || '',
  });
  plannedTimeRange.value = normalizedPlannedStartTime && normalizedPlannedEndTime
    ? [normalizedPlannedStartTime, normalizedPlannedEndTime]
    : [];
  loadNurseOptions();
  loadElderOptions();
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  if (dialogMode.value === 'create' && !canDispatchTask.value) {
    ElMessage.warning('暂无下发任务权限');
    return;
  }
  if (dialogMode.value === 'edit' && !canUpdateTask.value) {
    ElMessage.warning('暂无编辑任务权限');
    return;
  }
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  if (plannedTimeRange.value?.length === 2) {
    formData.plannedStartTime = normalizeDateTimeText(plannedTimeRange.value[0]);
    formData.plannedEndTime = normalizeDateTimeText(plannedTimeRange.value[1]);
  } else {
    formData.plannedStartTime = '';
    formData.plannedEndTime = '';
  }

  submitting.value = true;
  try {
    let res: any;
    if (dialogMode.value === 'create') {
      res = await dispatchNursingTask({ ...formData, sanaId: effectiveSanaId.value });
    } else {
      res = await updateNursingTask(formData as any);
    }
    if (res?.code === 200) {
      ElMessage.success(dialogMode.value === 'create' ? '下发成功' : '更新成功');
      dialogVisible.value = false;
      fetchData();
    } else {
      ElMessage.error(res?.message || '操作失败');
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败');
  }
  submitting.value = false;
};

const handleCancel = async (row: NursingTaskRow) => {
  if (!canCancelTask.value) {
    ElMessage.warning('暂无取消任务权限');
    return;
  }
  try {
    await ElMessageBox.confirm(`确定取消任务「${row.taskTitle}」吗？`, '取消确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    const res: any = await cancelNursingTask(row.id);
    if (res?.code === 200) {
      ElMessage.success('已取消');
      fetchData();
    } else {
      ElMessage.error(res?.message || '取消失败');
    }
  } catch { /* user cancel */ }
};

const handleReactivate = async (row: NursingTaskRow) => {
  if (!canReactivateTask.value) {
    ElMessage.warning('暂无激活任务权限');
    return;
  }
  try {
    await ElMessageBox.confirm(`确定重新激活任务「${row.taskTitle}」吗？`, '激活确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    const res: any = await reactivateNursingTask(row.id);
    if (res?.code === 200) {
      ElMessage.success('已激活');
      fetchData();
    } else {
      ElMessage.error(res?.message || '激活失败');
    }
  } catch { /* user cancel */ }
};

const handleRemove = async (row: NursingTaskRow) => {
  if (!canRemoveTask.value) {
    ElMessage.warning('暂无删除任务权限');
    return;
  }
  try {
    await ElMessageBox.confirm(`确定删除任务「${row.taskTitle}」吗？删除后任务将在列表中隐藏。`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    const res: any = await removeNursingTask(row.id);
    if (res?.code === 200) {
      ElMessage.success('已删除');
      fetchData();
    } else {
      ElMessage.error(res?.message || '删除失败');
    }
  } catch { /* user cancel */ }
};

onMounted(() => {
  loadDictOptions();
  loadSanatoriumOptions();
  if (!isMultiScope.value) {
    loadElderOptions();
    loadNurseOptions();
  }
  fetchData();
  startAutoRefresh();
});

onBeforeUnmount(() => {
  stopAutoRefresh();
});
</script>

<style scoped>
.nursing-task-page {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.page-desc {
  font-size: 13px;
  color: #909399;
  margin: 4px 0 0;
}
.query-card {
  margin-bottom: 16px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
