<template>
  <section
    class="vector-store-page"
    v-loading.fullscreen.lock="uploading"
    element-loading-text="知识库上传处理中，请稍候..."
    element-loading-background="rgba(16, 24, 40, 0.45)"
  >
    <div class="page-hero">
      <div class="hero-left">
        <h2>知识库管理</h2>
        <p>集中管理知识文档、知识分块与索引状态</p>
      </div>
      <div class="hero-right">
        <el-tag size="large" type="info" effect="plain">集合：{{ stats.collectionName || '--' }}</el-tag>
        <el-button @click="handleRefresh" :icon="Refresh" :loading="loading || statsLoading">刷新数据</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards" v-loading="statsLoading">
      <div class="stat-card stat-card--blue">
        <div class="stat-top">
          <span class="stat-title">文档总数</span>
          <el-tag size="small" effect="plain">文件</el-tag>
        </div>
        <div class="stat-value">{{ stats.totalDocuments }}</div>
        <div class="stat-desc">已上传文件数量</div>
      </div>
      <div class="stat-card stat-card--green">
        <div class="stat-top">
          <span class="stat-title">分块总数</span>
          <el-tag size="small" type="success" effect="plain">知识</el-tag>
        </div>
        <div class="stat-value">{{ formatNumber(stats.totalChunks) }}</div>
        <div class="stat-desc">知识分块总数</div>
      </div>
      <div class="stat-card stat-card--cyan">
        <div class="stat-top">
          <span class="stat-title">集合大小</span>
          <el-tag size="small" type="info" effect="plain">存储</el-tag>
        </div>
        <div class="stat-value">{{ formatNumber(stats.collectionSize) }}</div>
        <div class="stat-desc">已索引向量数</div>
      </div>
      <div class="stat-card stat-card--orange">
        <div class="stat-top">
          <span class="stat-title">集合名称</span>
          <el-tag size="small" type="warning" effect="plain">Qdrant</el-tag>
        </div>
        <div class="stat-value small">{{ stats.collectionName }}</div>
        <div class="stat-desc">知识索引集合</div>
      </div>
    </div>

    <div class="content-grid">
      <!-- 文档上传 -->
      <el-card v-if="canUploadDocument || canUploadBatchDocument" class="upload-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>
              <el-icon><Upload /></el-icon>
              文档上传
            </span>
            <el-text size="small" type="info">支持 PDF、Word、TXT</el-text>
          </div>
        </template>

        <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :disabled="uploading || !canSelectUploadFiles"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :before-upload="beforeUpload"
            :on-exceed="handleExceed"
            :limit="10"
            multiple
            class="compact-upload"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或<em>点击选择</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              单个文件最大 10MB，支持 .pdf / .doc / .docx / .txt
            </div>
          </template>
        </el-upload>

        <div class="upload-meta">
          <el-tag size="small" type="info" effect="plain">已选择 {{ selectedFileCount }} 个文件</el-tag>
          <el-text size="small" type="info">总大小 {{ formatFileSize(selectedTotalSize) }}</el-text>
        </div>

        <div v-if="showSanaSelector" class="sana-selector">
          <el-select
            v-model="selectedSanaId"
            placeholder="请选择上传目标机构"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="item in sanatoriumOptions"
              :key="item.id"
              :label="item.sanaName"
              :value="item.id"
            />
          </el-select>
          <el-text size="small" type="info">
            多机构账号上传时必须指定机构，避免文档归属错误
          </el-text>
        </div>

        <el-alert
          v-if="uploading"
          title="上传中：系统正在进行文档解析、分块与向量化"
          type="info"
          :closable="false"
          show-icon
        />

        <div class="upload-actions">
          <el-button type="primary" @click="handleUpload" :loading="uploading" :disabled="!canUpload">
            <el-icon><Upload /></el-icon>
            {{ uploading ? '上传处理中...' : '开始上传' }}
          </el-button>
          <el-button @click="handleClearFiles" :disabled="fileList.length === 0 || uploading">
            <el-icon><Delete /></el-icon>
            清空
          </el-button>
        </div>
      </el-card>

      <!-- 文档列表 -->
      <el-card class="list-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>
              <el-icon><Files /></el-icon>
              文档列表
            </span>
            <div class="header-actions">
              <el-input
                  v-model="searchFileName"
                  placeholder="搜索文件名"
                  clearable
                  style="width: 220px"
                  @keyup.enter="handleSearch"
                  class="search-input"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button @click="handleRefresh" :icon="Refresh">刷新</el-button>
              <el-button v-if="canClearVectorStore" type="danger" @click="handleClearAll" :icon="Delete" :disabled="stats.totalChunks === 0">
                清空知识库
              </el-button>
            </div>
          </div>
        </template>

        <div class="table-shell">
          <el-table
              :data="tableData"
              border
              stripe
              style="width: 100%"
              v-loading="loading"
              size="small"
              empty-text="暂无文档，请先上传知识文件"
          >
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
            <el-table-column prop="fileType" label="类型" width="90" align="center">
              <template #default="scope">
                <el-tag size="small" effect="light" :type="getFileTypeTagType(scope.row.fileType)">
                  {{ scope.row.fileType?.toUpperCase() || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="分块" width="90" align="center">
              <template #default="scope">
                <el-tag size="small" effect="plain">{{ scope.row.chunkIndex }}/{{ scope.row.totalChunks }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="contentLength" label="字符数" width="100" align="right">
              <template #default="scope">
                {{ formatNumber(scope.row.contentLength) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="center" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="handleViewContent(scope.row)" size="small">
                  查看
                </el-button>
                <el-button v-if="canDeleteDocument" link type="danger" @click="handleDeleteFile(scope.row.fileName)" size="small">
                  删除
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="当前无可展示文档" />
            </template>
          </el-table>

          <div class="pagination-row" v-if="total > 0">
            <el-pagination
                v-model:current-page="pageQuery.page"
                v-model:page-size="pageQuery.pageSize"
                :total="total"
                background
                layout="total, sizes, prev, pager, next"
                :page-sizes="[10, 20, 50]"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </el-card>
    </div>

    <!-- 内容查看弹窗 -->
    <el-dialog
        v-model="contentDialogVisible"
        title="文档内容"
        width="600px"
        :destroy-on-close="true"
    >
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="文件名">{{ currentDocument.fileName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ currentDocument.fileType }}</el-descriptions-item>
        <el-descriptions-item label="分块">
          {{ currentDocument.chunkIndex }} / {{ currentDocument.totalChunks }}
        </el-descriptions-item>
        <el-descriptions-item label="字符数">
          {{ formatNumber(currentDocument.contentLength) }}
        </el-descriptions-item>
        <el-descriptions-item label="文档ID" :span="2">{{ currentDocument.id }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">内容预览</el-divider>
      <div class="content-preview">{{ currentDocument.content || '无内容' }}</div>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, reactive, onMounted, computed } from 'vue';
import type { UploadInstance, UploadProps, UploadRawFile, UploadUserFile } from 'element-plus';
import { Refresh, Delete, Upload, UploadFilled, Files, Search } from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/userStore';
import { hasResourcePath } from '@/constants/authRoles';
import { pageSanatoriumAPI } from '@/api/sanatorium';
import {
  getVectorStoreStatsAPI,
  getDocumentsAPI,
  getDocumentsByFileNameAPI,
  deleteDocumentsByFileNameAPI,
  clearVectorStoreAPI,
  uploadDocumentAPI,
  uploadDocumentsBatchAPI
} from '@/api/vectorStore';
import type { VectorStoreStats, DocumentInfo } from '@/api/vectorStore';

interface SanatoriumOption {
  id: number;
  sanaName: string;
}

// 统计信息
const stats = ref<VectorStoreStats>({
  totalDocuments: 0,
  totalChunks: 0,
  collectionSize: 0,
  collectionName: ''
});

// 搜索文件名
const searchFileName = ref('');

// 分页查询参数
const pageQuery = reactive({
  page: 1,
  pageSize: 10
});

// 表格数据
const tableData = ref<DocumentInfo[]>([]);
const total = ref(0);
const loading = ref(false);
const statsLoading = ref(false);

// 内容查看弹窗
const contentDialogVisible = ref(false);
const currentDocument = ref<DocumentInfo>({
  id: '',
  fileName: '',
  fileType: '',
  chunkIndex: 0,
  totalChunks: 1,
  content: '',
  contentLength: 0,
  metadata: {}
});

// 上传相关
const uploadRef = ref<UploadInstance>();
const fileList = ref<UploadUserFile[]>([]);
const uploading = ref(false);
const selectedFileCount = computed(() => fileList.value.length);
const selectedTotalSize = computed(() => {
  return fileList.value.reduce((sum, file) => {
    const size = file.size ?? file.raw?.size ?? 0;
    return sum + size;
  }, 0);
});
const userStore = useUserStore();
const canUploadDocument = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/vector-store/upload'));
const canUploadBatchDocument = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/vector-store/upload/batch'));
const canSelectUploadFiles = computed(() => canUploadDocument.value || canUploadBatchDocument.value);
const canUpload = computed(() => {
  if (uploading.value || selectedFileCount.value === 0) {
    return false;
  }
  return selectedFileCount.value === 1 ? canUploadDocument.value : canUploadBatchDocument.value;
});
const canDeleteDocument = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/vector-store/documents/**'));
const canClearVectorStore = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/vector-store/clear'));
const sanatoriumOptions = ref<SanatoriumOption[]>([]);
const selectedSanaId = ref<number | undefined>(undefined);
const userSanaScopeIds = computed<number[]>(() => {
  return (userStore.userInfo.sanaScopeIds || []).filter((id): id is number => typeof id === 'number');
});
const showSanaSelector = computed(() => {
  return userSanaScopeIds.value.length > 1;
});

const resetUploadSelection = () => {
  fileList.value = [];
  uploadRef.value?.clearFiles();
};

// 文件选择变化
const handleFileChange: UploadProps['onChange'] = (uploadFile, uploadFiles) => {
  fileList.value = uploadFiles;
};

// 文件移除
const handleFileRemove: UploadProps['onRemove'] = (uploadFile, uploadFiles) => {
  fileList.value = uploadFiles;
};

// 初始化
onMounted(() => {
  initSanaSelector();
  fetchStats();
  fetchDocumentList();
});

const initSanaSelector = async () => {
  if (!showSanaSelector.value) {
    selectedSanaId.value = userSanaScopeIds.value.length === 1 ? userSanaScopeIds.value[0] : undefined;
    return;
  }
  await fetchSanatoriumOptions();
};

const fetchSanatoriumOptions = async () => {
  try {
    const res = await pageSanatoriumAPI({
      page: 1,
      pageSize: 200
    } as any);
    const records = (res?.code === 200 ? (res?.data?.records || []) : []) as SanatoriumOption[];
    sanatoriumOptions.value = records
      .filter(item => item && typeof item.id === 'number')
      .map(item => ({ id: item.id, sanaName: item.sanaName }));
    if (!selectedSanaId.value && sanatoriumOptions.value.length === 1) {
      selectedSanaId.value = sanatoriumOptions.value[0].id;
    }
  } catch (error) {
    console.error('加载机构列表失败:', error);
  }
};

// 获取统计信息
const fetchStats = async () => {
  try {
    statsLoading.value = true;
    const res = await getVectorStoreStatsAPI();
    if (res.code === 200 && res.data) {
      stats.value = res.data;
    }
  } catch (error) {
    console.error('获取统计信息错误:', error);
  } finally {
    statsLoading.value = false;
  }
};

// 获取文档列表
const fetchDocumentList = async () => {
  try {
    loading.value = true;
    const res = await getDocumentsAPI(pageQuery.page, pageQuery.pageSize);
    if (res.code === 200 && res.data) {
      tableData.value = res.data.documents || [];
      total.value = res.data.total || 0;
    }
  } catch (error) {
    console.error('获取文档列表错误:', error);
    ElMessage.error('获取文档列表失败');
  } finally {
    loading.value = false;
  }
};

// 搜索
const handleSearch = async () => {
  if (!searchFileName.value.trim()) {
    fetchDocumentList();
    return;
  }
  try {
    loading.value = true;
    const res = await getDocumentsByFileNameAPI(searchFileName.value.trim());
    if (res.code === 200 && res.data) {
      tableData.value = res.data || [];
      total.value = res.data?.length || 0;
    }
  } catch (error) {
    ElMessage.error('搜索失败');
  } finally {
    loading.value = false;
  }
};

// 刷新
const handleRefresh = () => {
  searchFileName.value = '';
  pageQuery.page = 1;
  fetchStats();
  fetchDocumentList();
};

// 分页
const handleSizeChange = (val: number) => {
  pageQuery.pageSize = val;
  fetchDocumentList();
};

const handleCurrentChange = (val: number) => {
  pageQuery.page = val;
  fetchDocumentList();
};

// 查看内容
const handleViewContent = (row: DocumentInfo) => {
  currentDocument.value = { ...row };
  contentDialogVisible.value = true;
};

// 删除文件
const handleDeleteFile = async (fileName: string) => {
  if (!canDeleteDocument.value) {
    ElMessage.warning('当前账号无删除知识库文档权限');
    return;
  }
  try {
    await ElMessageBox.confirm(`确定要删除文件 "${fileName}" 的所有分块吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    loading.value = true;
    const res = await deleteDocumentsByFileNameAPI(fileName);
    if (res.code === 200 && res.data !== undefined) {
      ElMessage.success(`删除成功，共 ${res.data} 个分块`);
      fetchStats();
      fetchDocumentList();
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败');
    }
  } finally {
    loading.value = false;
  }
};

// 清空知识库
const handleClearAll = async () => {
  if (!canClearVectorStore.value) {
    ElMessage.warning('当前账号无清空知识库权限');
    return;
  }
  try {
    await ElMessageBox.confirm('确定要清空知识库吗？此操作不可撤销！', '确认清空', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    });
    loading.value = true;
    const res = await clearVectorStoreAPI();
    if (res.code === 200 && res.data !== undefined) {
      ElMessage.success(`清空成功，共删除 ${res.data} 个分块`);
      fetchStats();
      fetchDocumentList();
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败');
    }
  } finally {
    loading.value = false;
  }
};

// 上传前校验
const beforeUpload = (rawFile: UploadRawFile) => {
  const fileName = rawFile.name;
  const validTypes = ['.pdf', '.doc', '.docx', '.txt'];
  const fileExt = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
  if (!validTypes.includes(fileExt)) {
    ElMessage.error('仅支持 PDF、Word、TXT 格式');
    return false;
  }
  if (rawFile.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 10MB');
    return false;
  }
  return true;
};

const handleExceed: UploadProps['onExceed'] = () => {
  ElMessage.warning('最多只能上传 10 个文件');
};

// 手动上传
const handleUpload = async () => {
  if (!canSelectUploadFiles.value) {
    ElMessage.warning('当前账号无上传知识库文档权限');
    return;
  }
  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择文件');
    return;
  }

  if (showSanaSelector.value && typeof selectedSanaId.value !== 'number') {
    ElMessage.warning('多机构账号请先选择上传目标机构');
    return;
  }

  uploading.value = true;
  try {
    const rawFiles = fileList.value
      .map(f => f.raw)
      .filter((f): f is UploadRawFile => f !== undefined);

    if (rawFiles.length === 0) {
      ElMessage.error('获取文件失败');
      return;
    }

    if (rawFiles.length === 1) {
      if (!canUploadDocument.value) {
        ElMessage.warning('当前账号无单文件上传权限');
        return;
      }
      const res = await uploadDocumentAPI(rawFiles[0] as File, selectedSanaId.value);
      if (res.code === 200) {
        ElMessage.success(res.message || '上传成功');
        resetUploadSelection();
        fetchStats();
        fetchDocumentList();
      } else {
        ElMessage.error(res.message || '上传失败');
      }
    } else {
      if (!canUploadBatchDocument.value) {
        ElMessage.warning('当前账号无批量上传权限');
        return;
      }
      const res = await uploadDocumentsBatchAPI(rawFiles as File[], selectedSanaId.value);
      if (res.code === 200) {
        ElMessage.success(res.message || '批量上传成功');
        resetUploadSelection();
        fetchStats();
        fetchDocumentList();
      } else {
        ElMessage.error(res.message || '批量上传失败');
      }
    }
  } catch (error: any) {
    if (error?.code === 'ECONNABORTED') {
      ElMessage.warning('上传处理时间较长，前端等待超时。请稍后刷新文档列表确认是否已入库。');
      return;
    }
    ElMessage.error(error.message || '上传失败');
  } finally {
    uploading.value = false;
  }
};

const handleClearFiles = () => {
  resetUploadSelection();
};

// 格式化数字
const formatNumber = (num: number): string => {
  return num.toLocaleString('zh-CN');
};

const formatFileSize = (size: number): string => {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`;
  return `${(size / (1024 * 1024)).toFixed(2)} MB`;
};

const getFileTypeTagType = (fileType: string): 'success' | 'warning' | 'info' => {
  const type = (fileType || '').toLowerCase();
  if (type === 'pdf') return 'warning';
  if (type === 'doc' || type === 'docx') return 'success';
  return 'info';
};
</script>

<style scoped>
.vector-store-page {
  padding: 18px;
  min-height: calc(100vh - 76px);
  background: #f5f7fa;
}

.page-hero {
  border: 1px solid #dae3ee;
  background: #ffffff;
  border-radius: 10px;
  padding: 16px 18px;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hero-left h2 {
  margin: 0;
  font-size: 18px;
  color: #1a2f4d;
}

.hero-left p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.hero-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}

.stat-card {
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 4px 14px rgba(26, 47, 77, 0.05);
  border: 1px solid #d7e0ea;
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
}

.stat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.stat-title {
  color: #5d728f;
  font-size: 12px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a2f4d;
  margin-bottom: 4px;
}

.stat-value.small {
  font-size: 14px;
  word-break: break-all;
}

.stat-desc {
  color: #8b95a5;
  font-size: 11px;
}

/* 内容网格 */
.content-grid {
  --panel-min-height: clamp(420px, calc(100vh - 290px), 680px);
  display: grid;
  grid-template-columns: minmax(300px, 340px) 1fr;
  gap: 18px;
  align-items: stretch;
}

/* 上传卡片 */
.upload-card {
  min-height: var(--panel-min-height);
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  border: 1px solid #d8e3f2;
}

.upload-card :deep(.el-card__header) {
  padding: 13px 16px;
  border-bottom: 1px solid #e6edf5;
}

.upload-card :deep(.el-card__body) {
  padding: 16px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.compact-upload :deep(.el-upload-dragger) {
  padding: 20px;
  min-height: 132px;
  border: 1px dashed #9cb8db;
  background: #f8fbff;
}

.compact-upload :deep(.el-upload-dragger:hover) {
  border-color: #409eff;
}

.el-icon--upload {
  font-size: 36px;
  color: #9ab4d6;
  margin: 10px 0 8px;
}

.el-upload__text {
  font-size: 13px;
  color: #606266;
}

.el-upload__text em {
  color: #409eff;
  font-style: normal;
}

.el-upload__tip {
  font-size: 11px;
  color: #909399;
  margin-top: 8px;
}

.upload-meta {
  margin-top: 10px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.sana-selector {
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.upload-actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px dashed #e6edf5;
}

.upload-actions .el-button {
  flex: 1;
}

/* 列表卡片 */
.list-card {
  min-height: var(--panel-min-height);
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  border: 1px solid #d8e3f2;
}

.list-card :deep(.el-card__header) {
  padding: 12px 16px;
  border-bottom: 1px solid #e6edf5;
}

.list-card :deep(.el-card__body) {
  padding: 16px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.card-header span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-input {
  flex-shrink: 0;
}

.table-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.table-shell :deep(.el-table) {
  flex: 1;
}

.table-shell :deep(.el-table__empty-block) {
  min-height: clamp(240px, 32vh, 360px);
}

.list-card :deep(.el-table th.el-table__cell) {
  background: #f3f7fc;
  color: #334155;
}

.list-card :deep(.el-table .el-table__row:hover > td.el-table__cell) {
  background: #f8fbff;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 内容预览 */
.content-preview {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
  line-height: 1.6;
  color: #606266;
  font-size: 13px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .page-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-right {
    width: 100%;
    justify-content: space-between;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .upload-card,
  .list-card {
    min-height: auto;
    height: auto;
  }

  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .vector-store-page {
    padding: 12px;
  }

  .hero-right {
    flex-direction: column;
    align-items: stretch;
  }

  .upload-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-cards {
    grid-template-columns: 1fr;
  }

  .header-actions {
    flex-wrap: wrap;
  }
}
</style>
