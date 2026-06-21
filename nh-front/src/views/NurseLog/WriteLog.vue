<template>
  <div class="write-log-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">护理日志</h2>
        <p class="page-desc">编写与查看我的护理日志</p>
      </div>
      <el-button v-if="canAddLog" type="primary" @click="openAddDialog">新增日志</el-button>
    </div>

    <!-- 筛选 -->
    <el-card shadow="never" class="query-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="日志内容">
          <el-input v-model.trim="queryForm.content" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="是否异常">
          <el-select v-model="queryForm.abnormalFlag" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="0" />
            <el-option label="异常" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="日志时间">
          <el-date-picker
            v-model="logTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 360px"
          />
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
        <el-table-column prop="taskTitle" label="关联任务" min-width="140" show-overflow-tooltip />
        <el-table-column prop="elderName" label="关联老人" width="100" show-overflow-tooltip />
        <el-table-column prop="logTime" label="日志时间" width="190">
          <template #default="{ row }">{{ formatDisplayDateTime(row.logTime) }}</template>
        </el-table-column>
        <el-table-column prop="abnormalFlag" label="异常标记" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.abnormalFlag === 1 ? 'danger' : 'success'" size="small">
              {{ row.abnormalFlag === 1 ? '异常' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="日志内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canUpdateLog" link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增日志' : '编辑日志'" width="600px" destroy-on-close>
      <el-alert v-if="prefillNotice" :title="prefillNotice" type="info" :closable="false" show-icon class="prefill-alert" />
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="关联任务">
          <el-select v-model="formData.taskId" placeholder="请选择任务（可选）" clearable filterable style="width: 100%">
            <el-option v-for="t in myTaskOptions" :key="t.id" :label="t.taskTitle" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联老人">
          <el-select v-model="formData.elderId" placeholder="请选择老人（可选）" clearable filterable style="width: 100%">
            <el-option v-for="e in elderOptions" :key="e.id" :label="e.displayLabel" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日志时间" prop="logTime">
          <el-date-picker v-model="formData.logTime" type="datetime" placeholder="请选择" value-format="YYYY-MM-DD HH:mm:ss" :disabled-date="disableFutureDate" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日志内容" prop="content">
          <el-input v-model="formData.content" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="是否异常">
          <el-switch v-model="abnormalSwitch" active-text="异常" inactive-text="正常" />
        </el-form-item>
        <el-form-item label="日志附件">
          <div class="attachment-field">
            <div class="attachment-toolbar">
              <el-upload
                v-if="canUploadFile"
                :auto-upload="false"
                :show-file-list="false"
                multiple
                :disabled="submitting || uploadingAttachments"
                @change="handleAttachmentSelect"
              >
                <el-button :loading="uploadingAttachments">选择本地文件</el-button>
              </el-upload>
              <el-button
                v-if="canUploadFile"
                type="primary"
                plain
                :disabled="pendingAttachmentFiles.length === 0 || uploadingAttachments || submitting"
                :loading="uploadingAttachments"
                @click="handleUploadNow"
              >
                立即上传
              </el-button>
            </div>
            <div v-if="canUploadFile" class="attachment-tip">先选择文件，再点“立即上传”。</div>
            <div v-if="attachmentUrls.length > 0" class="attachment-list">
              <div v-for="(url, index) in attachmentUrls" :key="`${url}-${index}`" class="attachment-item">
                <el-link type="primary" underline="never" @click="previewAttachment(url)">
                  {{ getAttachmentName(url, index) }}
                </el-link>
                <el-button v-if="canDeleteFile" link type="danger" size="small" @click="removeExistingAttachment(index)">移除</el-button>
              </div>
            </div>
            <div v-if="pendingAttachmentFiles.length > 0" class="attachment-list">
              <div
                v-for="(file, index) in pendingAttachmentFiles"
                :key="`${file.name}-${file.size}-${index}`"
                class="attachment-item"
              >
                <span>{{ file.name }}</span>
                <el-tag size="small" type="info">待上传</el-tag>
                <el-button v-if="canUploadFile" link type="danger" size="small" @click="removePendingAttachment(index)">移除</el-button>
              </div>
            </div>
          </div>
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
import { computed, reactive, ref, onMounted, watch } from 'vue';
import type { FormInstance, FormRules, UploadFile } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import {
  getMyNursingLogPage,
  addNursingLog,
  updateNursingLog,
  type NursingLogRow,
  type NursingLogAdd,
} from '@/api/nursingLog';
import { getMyNursingTaskPage } from '@/api/nursingTask';
import { getElderPage } from '@/api/elder';
import { deleteFile, downloadFileByUrl, extractObjectKeyFromUrl, upload } from '@/api/file';
import { formatDisplayDateTime, normalizeDateTimeText } from '@/utils/dateTime';
import { useUserStore } from '@/stores/userStore';
import { hasResourcePath } from '@/constants/authRoles';

interface TaskOption { id: number; taskTitle: string }
interface ElderOption { id: number; elderName: string; displayLabel: string }

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const canAddLog = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-log/add'));
const canUpdateLog = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-log/update/**'));
const canUploadFile = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/commonFile/upload'));
const canDeleteFile = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/commonFile/delete'));

const buildElderLabel = (r: any): string => {
  const parts: string[] = [];
  if (r.sex != null) parts.push(r.sex === 1 ? '男' : '女');
  if (r.age != null) parts.push(`${r.age}岁`);
  return parts.length > 0 ? `${r.elderName || ''}（${parts.join(' / ')}）` : (r.elderName || '');
};

const normalizeQueryNumber = (value: unknown) => {
  if (typeof value !== 'string' || value.trim() === '') return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
};

const clearTaskPrefillQuery = async () => {
  const nextQuery = { ...route.query };
  delete nextQuery.fromTaskComplete;
  delete nextQuery.taskId;
  delete nextQuery.taskTitle;
  delete nextQuery.elderId;
  delete nextQuery.logTime;
  await router.replace({ path: route.path, query: nextQuery });
};

const loading = ref(false);
const tableData = ref<NursingLogRow[]>([]);
const total = ref(0);

const myTaskOptions = ref<TaskOption[]>([]);
const elderOptions = ref<ElderOption[]>([]);

const queryForm = reactive({
  content: '',
  abnormalFlag: undefined as number | undefined,
});

const logTimeRange = ref<string[]>([]);
const pageQuery = reactive({ page: 1, pageSize: 10 });

const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'edit'>('create');
const submitting = ref(false);
const uploadingAttachments = ref(false);
const deletingAttachment = ref(false);
const formRef = ref<FormInstance>();
const editingId = ref<number | null>(null);
const abnormalSwitch = ref(false);
const prefillNotice = ref('');
const pageInitialized = ref(false);
const handlingTaskPrefill = ref(false);
const attachmentUrls = ref<string[]>([]);
const pendingAttachmentFiles = ref<File[]>([]);

const defaultForm = (): NursingLogAdd => ({
  taskId: undefined as unknown as number,
  elderId: undefined as unknown as number,
  logTime: '',
  content: '',
  abnormalFlag: 0,
  attachmentUrls: '',
});

const formData = reactive<NursingLogAdd>(defaultForm());

const disableFutureDate = (date: Date) => date.getTime() > Date.now();

const formRules: FormRules = {
  logTime: [{ required: true, message: '请选择日志时间', trigger: 'change' }],
  content: [{ required: true, message: '请输入日志内容', trigger: 'blur' }],
};

const loadTaskOptions = async () => {
  try {
    const res: any = await getMyNursingTaskPage({ page: 1, pageSize: 999 });
    if (res?.code === 200 && res?.data?.records) {
      myTaskOptions.value = res.data.records.map((r: any) => ({ id: r.id, taskTitle: r.taskTitle }));
    }
  } catch { /* ignore */ }
};

const loadElderOptions = async () => {
  try {
    const res: any = await getElderPage({ page: 1, pageSize: 999 });
    if (res?.code === 200 && res?.data?.records) {
      elderOptions.value = res.data.records.map((r: any) => ({ id: r.id, elderName: r.elderName, displayLabel: buildElderLabel(r) }));
    }
  } catch { /* ignore */ }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params: any = { ...pageQuery, ...queryForm };
    if (logTimeRange.value?.length === 2) {
      params.logTimeBegin = logTimeRange.value[0];
      params.logTimeEnd = logTimeRange.value[1];
    }
    const res: any = await getMyNursingLogPage(params);
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
  queryForm.content = '';
  queryForm.abnormalFlag = undefined;
  logTimeRange.value = [];
  pageQuery.page = 1;
  fetchData();
};

const openAddDialog = () => {
  if (!canAddLog.value) {
    ElMessage.warning('暂无新增日志权限');
    return;
  }
  dialogMode.value = 'create';
  Object.assign(formData, defaultForm());
  abnormalSwitch.value = false;
  editingId.value = null;
  prefillNotice.value = '';
  attachmentUrls.value = [];
  pendingAttachmentFiles.value = [];
  dialogVisible.value = true;
};

const openEditDialog = (row: NursingLogRow) => {
  if (!canUpdateLog.value) {
    ElMessage.warning('暂无编辑日志权限');
    return;
  }
  dialogMode.value = 'edit';
  editingId.value = row.id;
  Object.assign(formData, {
    taskId: row.taskId,
    elderId: row.elderId,
    logTime: normalizeDateTimeText(row.logTime),
    content: row.content,
    abnormalFlag: row.abnormalFlag,
    attachmentUrls: row.attachmentUrls || '',
  });
  abnormalSwitch.value = row.abnormalFlag === 1;
  prefillNotice.value = '';
  attachmentUrls.value = splitAttachmentUrls(row.attachmentUrls);
  pendingAttachmentFiles.value = [];
  dialogVisible.value = true;
};

const openTaskPrefilledAddDialog = (taskId?: number, elderId?: number, taskTitle?: string, logTime?: string) => {
  if (!canAddLog.value) {
    ElMessage.warning('暂无新增日志权限');
    return;
  }
  dialogMode.value = 'create';
  Object.assign(formData, {
    ...defaultForm(),
    taskId,
    elderId,
    logTime: normalizeDateTimeText(logTime),
  });
  abnormalSwitch.value = false;
  editingId.value = null;
  prefillNotice.value = taskTitle
    ? `任务「${taskTitle}」已完成，请补充护理日志后保存。`
    : '任务已完成，请补充护理日志后保存。';
  attachmentUrls.value = [];
  pendingAttachmentFiles.value = [];
  dialogVisible.value = true;
};

const splitAttachmentUrls = (raw?: string) => {
  return String(raw || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
};

const syncAttachmentUrlsToForm = () => {
  formData.attachmentUrls = attachmentUrls.value.join(',');
};

const getAttachmentName = (url: string, index: number) => {
  try {
    const cleanUrl = url.split('?')[0];
    const fileName = decodeURIComponent(cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1));
    return fileName || `附件${index + 1}`;
  } catch {
    return `附件${index + 1}`;
  }
};

const previewAttachment = (url: string) => {
  downloadFileByUrl(url).catch((error: any) => {
    ElMessage.error(error?.message || '附件下载失败');
  });
};

const removeExistingAttachment = async (index: number) => {
  if (!canDeleteFile.value) {
    ElMessage.warning('暂无删除附件权限');
    return;
  }
  const targetUrl = attachmentUrls.value[index];
  if (!targetUrl) return;

  try {
    await ElMessageBox.confirm('删除后将同步清理 RustFS 中的原始文件，且不可恢复。是否继续？', '删除附件', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }

  const nextAttachmentUrls = attachmentUrls.value.filter((_, currentIndex) => currentIndex !== index);
  const nextAttachmentText = nextAttachmentUrls.join(',');
  const objectKey = extractObjectKeyFromUrl(targetUrl);

  deletingAttachment.value = true;
  try {
    if (dialogMode.value === 'edit' && editingId.value) {
      const updateRes: any = await updateNursingLog(editingId.value, {
        taskId: formData.taskId,
        elderId: formData.elderId,
        logTime: normalizeDateTimeText(formData.logTime),
        content: formData.content,
        abnormalFlag: abnormalSwitch.value ? 1 : 0,
        attachmentUrls: nextAttachmentText,
      });
      if (updateRes?.code !== 200) {
        throw new Error(updateRes?.message || '日志附件关系更新失败');
      }
    }

    if (objectKey) {
      const deleteRes: any = await deleteFile(objectKey);
      if (deleteRes?.code !== 200) {
        throw new Error(deleteRes?.message || 'RustFS 附件删除失败');
      }
    }

    attachmentUrls.value = nextAttachmentUrls;
    formData.attachmentUrls = nextAttachmentText;
    ElMessage.success('附件已硬删除');
    if (dialogMode.value === 'edit') {
      fetchData();
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '附件删除失败');
  } finally {
    deletingAttachment.value = false;
  }
};

const removePendingAttachment = (index: number) => {
  if (!canUploadFile.value) {
    ElMessage.warning('暂无上传附件权限');
    return;
  }
  pendingAttachmentFiles.value.splice(index, 1);
};

const handleAttachmentSelect = (uploadFile: UploadFile) => {
  if (!canUploadFile.value) {
    ElMessage.warning('暂无上传附件权限');
    return;
  }
  const rawFile = uploadFile.raw;
  if (!rawFile) return;
  const duplicated = pendingAttachmentFiles.value.some(
    (file) => file.name === rawFile.name && file.size === rawFile.size && file.lastModified === rawFile.lastModified
  );
  if (duplicated) {
    ElMessage.warning(`文件「${rawFile.name}」已选择`);
    return;
  }
  pendingAttachmentFiles.value.push(rawFile);
};

const uploadPendingAttachments = async () => {
  if (pendingAttachmentFiles.value.length === 0) {
    syncAttachmentUrlsToForm();
    return;
  }
  if (!canUploadFile.value) {
    throw new Error('暂无上传附件权限');
  }
  uploadingAttachments.value = true;
  try {
    for (const file of pendingAttachmentFiles.value) {
      const form = new FormData();
      form.append('file', file);
      const res: any = await upload(form);
      if (res?.code !== 200 || !res?.data) {
        throw new Error(res?.message || `附件上传失败: ${file.name}`);
      }
      attachmentUrls.value.push(res.data);
    }
    pendingAttachmentFiles.value = [];
    syncAttachmentUrlsToForm();
  } finally {
    uploadingAttachments.value = false;
  }
};

const handleUploadNow = async () => {
  if (!canUploadFile.value) {
    ElMessage.warning('暂无上传附件权限');
    return;
  }
  try {
    await uploadPendingAttachments();
    ElMessage.success('附件上传成功');
  } catch (error: any) {
    ElMessage.error(error?.message || '附件上传失败');
  }
};

const handleTaskPrefillFromRoute = async () => {
  if (handlingTaskPrefill.value || route.query.fromTaskComplete !== '1') {
    return;
  }

  handlingTaskPrefill.value = true;
  try {
    const taskId = normalizeQueryNumber(route.query.taskId);
    const elderId = normalizeQueryNumber(route.query.elderId);
    const taskTitle = typeof route.query.taskTitle === 'string' ? route.query.taskTitle : '';
    const logTime = typeof route.query.logTime === 'string' ? route.query.logTime : '';

    if (!taskId) {
      await clearTaskPrefillQuery();
      return;
    }

    const res: any = await getMyNursingLogPage({ page: 1, pageSize: 1, taskId });
    const existingRow: NursingLogRow | undefined = res?.code === 200 ? res?.data?.records?.[0] : undefined;

    if (existingRow) {
      if (!canUpdateLog.value) {
        ElMessage.warning('暂无编辑日志权限');
        await clearTaskPrefillQuery();
        return;
      }
      openEditDialog(existingRow);
      prefillNotice.value = taskTitle
        ? `已找到任务「${taskTitle}」的日志记录，请确认后保存。`
        : '已找到该任务的日志记录，请确认后保存。';
    } else {
      if (!canAddLog.value) {
        ElMessage.warning('暂无新增日志权限');
        await clearTaskPrefillQuery();
        return;
      }
      openTaskPrefilledAddDialog(taskId, elderId, taskTitle, logTime);
    }

    await clearTaskPrefillQuery();
  } finally {
    handlingTaskPrefill.value = false;
  }
};

const handleSubmit = async () => {
  if (dialogMode.value === 'create' && !canAddLog.value) {
    ElMessage.warning('暂无新增日志权限');
    return;
  }
  if (dialogMode.value === 'edit' && !canUpdateLog.value) {
    ElMessage.warning('暂无编辑日志权限');
    return;
  }
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  formData.abnormalFlag = abnormalSwitch.value ? 1 : 0;

  submitting.value = true;
  try {
    await uploadPendingAttachments();
    const payload = {
      ...formData,
      logTime: normalizeDateTimeText(formData.logTime),
      attachmentUrls: formData.attachmentUrls,
    };
    let res: any;
    if (dialogMode.value === 'create') {
      res = await addNursingLog(payload);
    } else {
      res = await updateNursingLog(editingId.value!, {
        taskId: payload.taskId,
        elderId: payload.elderId,
        logTime: payload.logTime,
        content: payload.content,
        abnormalFlag: payload.abnormalFlag,
        attachmentUrls: payload.attachmentUrls,
      });
    }
    if (res?.code === 200) {
      ElMessage.success(dialogMode.value === 'create' ? '新增成功' : '更新成功');
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

watch(
  () => route.fullPath,
  async () => {
    if (!pageInitialized.value) return;
    await handleTaskPrefillFromRoute();
  }
);

onMounted(async () => {
  await Promise.all([loadTaskOptions(), loadElderOptions(), fetchData()]);
  pageInitialized.value = true;
  await handleTaskPrefillFromRoute();
});
</script>

<style scoped>
.write-log-page {
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
.prefill-alert {
  margin-bottom: 16px;
}
.attachment-field {
  width: 100%;
}
.attachment-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.attachment-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
.attachment-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.attachment-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 24px;
  color: #606266;
  word-break: break-all;
}
</style>
