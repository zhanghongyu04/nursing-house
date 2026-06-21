<template>
  <div class="nursing-log-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">护理日志</h2>
        <p class="page-desc">查看本机构的护理日志记录</p>
      </div>
      <div v-if="canExportLog" class="header-actions">
        <el-button type="primary" plain :loading="exporting" @click="openExportDialog('filter')">导出筛选结果</el-button>
        <el-button
          type="success"
          plain
          :loading="exporting"
          :disabled="selectedRows.length === 0"
          @click="openExportDialog('selected')"
        >
          导出选中（{{ selectedRows.length }}）
        </el-button>
      </div>
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
            :default-time="logDefaultTime"
            :shortcuts="logShortcuts"
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
      <el-table :data="tableData" border stripe v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column v-if="canExportLog" type="selection" width="50" />
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="taskTitle" label="关联任务" min-width="140" show-overflow-tooltip />
        <el-table-column prop="elderName" label="关联老人" width="100" show-overflow-tooltip />
        <el-table-column prop="nurseUsername" label="护理人员" width="100" show-overflow-tooltip />
        <el-table-column prop="logTime" label="日志时间" width="170" />
        <el-table-column prop="abnormalFlag" label="异常标记" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.abnormalFlag === 1 ? 'danger' : 'success'" size="small">
              {{ row.abnormalFlag === 1 ? '异常' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="日志内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">查看</el-button>
            <el-button v-if="canExportLog" link type="success" size="small" @click="openExportDialog('single', row)">导出</el-button>
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

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="关联任务">{{ detailRow?.taskTitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联老人">{{ detailRow?.elderName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="护理人员">{{ detailRow?.nurseUsername || '-' }}</el-descriptions-item>
        <el-descriptions-item label="日志时间">{{ detailRow?.logTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="异常标记">
          <el-tag :type="detailRow?.abnormalFlag === 1 ? 'danger' : 'success'" size="small">
            {{ detailRow?.abnormalFlag === 1 ? '异常' : '正常' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属机构">{{ detailRow?.sanaName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="日志内容" :span="2">{{ detailRow?.content || '-' }}</el-descriptions-item>
        <el-descriptions-item label="日志附件" :span="2">
          <div v-if="currentAttachmentUrls.length > 0" class="attachment-list">
            <el-link
              v-for="(url, index) in currentAttachmentUrls"
              :key="`${url}-${index}`"
              type="primary"
              underline="never"
              @click="previewAttachment(url)"
            >
              {{ getAttachmentName(url, index) }}
            </el-link>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-if="canExportLog" v-model="exportVisible" title="导出日志" width="460px">
      <el-form label-width="110px">
        <el-form-item label="导出范围">
          <span>{{ exportScopeText }}</span>
        </el-form-item>
        <el-form-item label="报告格式">
          <el-radio-group v-model="exportForm.reportFormat">
            <el-radio value="docx">DOCX</el-radio>
            <el-radio value="pdf">PDF</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="导出附件">
          <el-switch v-model="exportForm.includeAttachments" active-text="包含附件" inactive-text="仅报告" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" :loading="exporting" @click="handleExport">开始导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { exportNursingLog, getNursingLogPage, type NursingLogRow } from '@/api/nursingLog';
import { downloadFileByUrl } from '@/api/file';
import { useUserStore } from '@/stores/userStore';
import { hasResourcePath } from '@/constants/authRoles';

const loading = ref(false);
const tableData = ref<NursingLogRow[]>([]);
const total = ref(0);
const userStore = useUserStore();
const canExportLog = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-log/export'));

const queryForm = reactive({
  content: '',
  abnormalFlag: undefined as number | undefined,
});

const logTimeRange = ref<string[]>([]);
const pageQuery = reactive({ page: 1, pageSize: 10 });

const logDefaultTime: [Date, Date] = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59),
];

const logShortcuts = [
  {
    text: '今天',
    value: () => {
      const s = new Date(); s.setHours(0, 0, 0, 0);
      const e = new Date(); e.setHours(23, 59, 59, 0);
      return [s, e];
    },
  },
  {
    text: '最近7天',
    value: () => {
      const e = new Date(); e.setHours(23, 59, 59, 0);
      const s = new Date(); s.setDate(s.getDate() - 6); s.setHours(0, 0, 0, 0);
      return [s, e];
    },
  },
  {
    text: '最近30天',
    value: () => {
      const e = new Date(); e.setHours(23, 59, 59, 0);
      const s = new Date(); s.setDate(s.getDate() - 29); s.setHours(0, 0, 0, 0);
      return [s, e];
    },
  },
];

const detailVisible = ref(false);
const detailRow = ref<NursingLogRow | null>(null);
const exporting = ref(false);
const exportVisible = ref(false);
const selectedRows = ref<NursingLogRow[]>([]);
type ExportScope = 'filter' | 'selected' | 'single';
const exportScope = ref<ExportScope>('filter');
const exportTargetIds = ref<number[]>([]);
const exportForm = reactive({
  reportFormat: 'docx' as 'docx' | 'pdf',
  includeAttachments: true,
});
const exportScopeText = computed(() => {
  if (exportScope.value === 'selected') return `多选导出（${exportTargetIds.value.length} 条）`;
  if (exportScope.value === 'single') return '单条导出（1 条）';
  return '当前筛选结果';
});
const currentAttachmentUrls = computed(() => splitAttachmentUrls(detailRow.value?.attachmentUrls));

const splitAttachmentUrls = (raw?: string) => {
  return String(raw || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
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

const fetchData = async () => {
  loading.value = true;
  try {
    const params: any = { ...pageQuery, ...queryForm };
    if (logTimeRange.value?.length === 2) {
      params.logTimeBegin = logTimeRange.value[0];
      params.logTimeEnd = logTimeRange.value[1];
    }
    const res: any = await getNursingLogPage(params);
    if (res?.code === 200 && res?.data) {
      tableData.value = res.data.records || [];
      total.value = res.data.total || 0;
      selectedRows.value = [];
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

const viewDetail = (row: NursingLogRow) => {
  detailRow.value = row;
  detailVisible.value = true;
};

const openExportDialog = (scope: ExportScope, row?: NursingLogRow) => {
  if (!canExportLog.value) {
    ElMessage.warning('暂无导出日志权限');
    return;
  }
  if (scope === 'selected') {
    const ids = selectedRows.value.map(item => item.id).filter((id): id is number => typeof id === 'number');
    if (ids.length === 0) {
      ElMessage.warning('请先勾选要导出的日志');
      return;
    }
    exportTargetIds.value = Array.from(new Set(ids));
  } else if (scope === 'single') {
    if (!row?.id) {
      ElMessage.warning('当前日志ID无效，无法导出');
      return;
    }
    exportTargetIds.value = [row.id];
  } else {
    exportTargetIds.value = [];
  }
  exportScope.value = scope;
  exportVisible.value = true;
};

const handleSelectionChange = (rows: NursingLogRow[]) => {
  if (!canExportLog.value) {
    selectedRows.value = [];
    return;
  }
  selectedRows.value = rows;
};

const parseFileNameFromContentDisposition = (contentDisposition?: string) => {
  if (!contentDisposition) return '';
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) return decodeURIComponent(utf8Match[1]);
  const normalMatch = contentDisposition.match(/filename=\"?([^\";]+)\"?/i);
  if (normalMatch?.[1]) return normalMatch[1];
  return '';
};

const handleExport = async () => {
  if (!canExportLog.value) {
    ElMessage.warning('暂无导出日志权限');
    return;
  }
  exporting.value = true;
  try {
    const params: any = {
      reportFormat: exportForm.reportFormat,
      includeAttachments: exportForm.includeAttachments
    };

    if (exportScope.value === 'filter') {
      params.content = queryForm.content;
      params.abnormalFlag = queryForm.abnormalFlag;
      if (logTimeRange.value?.length === 2) {
        params.logTimeBegin = logTimeRange.value[0];
        params.logTimeEnd = logTimeRange.value[1];
      }
    } else {
      params.logIds = exportTargetIds.value;
    }

    const response: any = await exportNursingLog(params);
    const blob = response?.data instanceof Blob
      ? response.data
      : new Blob([response?.data], { type: 'application/octet-stream' });
    const contentDisposition = response?.headers?.['content-disposition'];
    const fileName = parseFileNameFromContentDisposition(contentDisposition) || `nursing-log-export-${Date.now()}.zip`;
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    exportVisible.value = false;
    ElMessage.success('导出成功');
  } catch (error: any) {
    ElMessage.error(error?.message || '导出失败');
  } finally {
    exporting.value = false;
  }
};

onMounted(() => fetchData());
</script>

<style scoped>
.nursing-log-page {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.header-actions {
  display: flex;
  gap: 8px;
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
.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
