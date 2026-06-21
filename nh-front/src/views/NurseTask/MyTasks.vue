<template>
  <div class="my-task-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">我的护理任务</h2>
        <p class="page-desc">查看分配给我的护理任务</p>
      </div>
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
        <el-table-column prop="taskContent" label="任务内容" min-width="180" show-overflow-tooltip />
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
        <el-table-column prop="plannedStartTime" label="计划开始" width="170" />
        <el-table-column prop="plannedEndTime" label="计划结束" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="canCompleteTask && (row.status === 0 || row.status === 1 || row.status === 4)" link type="primary" size="small" @click="handleComplete(row)">完成</el-button>
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
      :show-assignee="false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import { getMyNursingTaskPage, completeNursingTask, type NursingTaskRow } from '@/api/nursingTask';
import { getDictItemsBatch } from '@/api/dict';
import { useUserStore } from '@/stores/userStore';
import { hasResourcePath } from '@/constants/authRoles';
import NursingTaskDetailDialog from '@/components/NursingTaskDetailDialog.vue';

interface DictOption { value: number; label: string }
interface StringOption { value: string; label: string; sortNo?: number }

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
  return { value, label, sortNo: Number.isFinite(sortNo) ? sortNo : 0 };
};

const loading = ref(false);
const tableData = ref<NursingTaskRow[]>([]);
const total = ref(0);
const router = useRouter();
const userStore = useUserStore();
const canCompleteTask = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-task/complete/**'));
const detailVisible = ref(false);
const detailRow = ref<NursingTaskRow | null>(null);

const queryForm = reactive({
  taskTitle: '',
  taskType: undefined as number | undefined,
  status: undefined as number | undefined,
});

const pageQuery = reactive({ page: 1, pageSize: 10 });

const dictOptions = reactive<Record<string, DictOption[]>>({
  status: [],
  priority: [],
  taskType: [],
});
const tagStyleOptions = ref<StringOption[]>([]);

const formatCurrentDateTime = () => {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
};

const dictLabel = (opts: DictOption[], val: number) => opts.find(o => o.value === val)?.label ?? '-';

const getTagType = (scope: 'PRIORITY' | 'STATUS', value: number, fallback: string) => {
  const key = `${scope}_${value}`;
  const target = tagStyleOptions.value.find((o) => o.value === key);
  return target?.label || fallback;
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
    const res: any = await getDictItemsBatch(['NURSING_TASK_STATUS', 'NURSING_TASK_PRIORITY', 'NURSING_TASK_TYPE', 'NURSING_TASK_TAG_STYLE']);
    const map = res?.data || {};
    dictOptions.status = Array.isArray(map.NURSING_TASK_STATUS) ? map.NURSING_TASK_STATUS.map(toNumberOption).filter(Boolean) as DictOption[] : [];
    dictOptions.priority = Array.isArray(map.NURSING_TASK_PRIORITY) ? map.NURSING_TASK_PRIORITY.map(toNumberOption).filter(Boolean) as DictOption[] : [];
    dictOptions.taskType = Array.isArray(map.NURSING_TASK_TYPE) ? map.NURSING_TASK_TYPE.map(toNumberOption).filter(Boolean) as DictOption[] : [];
    tagStyleOptions.value = Array.isArray(map.NURSING_TASK_TAG_STYLE)
      ? (map.NURSING_TASK_TAG_STYLE.map(toStringOption).filter(Boolean) as StringOption[]).sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0))
      : [];
  } catch { /* ignore */ }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res: any = await getMyNursingTaskPage({ ...pageQuery, ...queryForm });
    if (res?.code === 200 && res?.data) {
      tableData.value = res.data.records || [];
      total.value = res.data.total || 0;
    }
  } catch { /* ignore */ }
  loading.value = false;
};

const handleSearch = () => {
  pageQuery.page = 1;
  fetchData();
};

const handleReset = () => {
  queryForm.taskTitle = '';
  queryForm.taskType = undefined;
  queryForm.status = undefined;
  pageQuery.page = 1;
  fetchData();
};

const viewDetail = (row: NursingTaskRow) => {
  detailRow.value = row;
  detailVisible.value = true;
};

const handleComplete = async (row: NursingTaskRow) => {
  if (!canCompleteTask.value) {
    ElMessage.warning('暂无完成任务权限');
    return;
  }
  try {
    await ElMessageBox.confirm(`确定完成任务「${row.taskTitle}」吗？`, '完成确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    });
    const res: any = await completeNursingTask(row.id);
    if (res?.code === 200) {
      ElMessage.success('任务已完成，正在打开日志编写');
      await router.push({
        name: 'WriteNursingLog',
        query: {
          fromTaskComplete: '1',
          taskId: String(row.id),
          taskTitle: row.taskTitle,
          elderId: row.elderId != null ? String(row.elderId) : '',
          logTime: formatCurrentDateTime(),
        },
      });
    } else {
      ElMessage.error(res?.message || '操作失败');
    }
  } catch { /* user cancel */ }
};

onMounted(() => {
  loadDictOptions();
  fetchData();
});
</script>

<style scoped>
.my-task-page {
  padding: 20px;
}
.page-header {
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
