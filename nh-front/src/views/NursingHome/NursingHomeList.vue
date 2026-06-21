<template>
  <div class="sana-page">
    <section class="top-banner card-shell">
      <div class="banner-main">
        <p class="banner-kicker">Operations Console</p>
        <div class="banner-title-row">
          <h1>康养机构信息</h1>
          <span class="banner-chip">机构管理</span>
        </div>
        <p class="banner-desc">
          面向机构运维与监管的统一工作台，集中完成检索、维护、导入导出和机构档案查看。
        </p>
      </div>
    </section>

    <section class="workbench-grid">
      <div class="main-column">
        <section class="card-shell filter-card">
          <div class="section-head">
            <div>
              <h2>筛选与批量操作</h2>
              <p>筛选目标机构并执行新增、编辑、删除、导入导出。</p>
            </div>
          </div>

          <el-form label-position="top" class="filters-form">
            <div class="filters-grid">
              <el-form-item label="机构名称">
                <el-input v-model="searchParams.sanaName" clearable placeholder="请输入机构名称" />
              </el-form-item>
              <el-form-item label="所属区划">
                <el-input v-model="searchParams.sanaAffiliation" clearable placeholder="请输入所属区划" />
              </el-form-item>
              <el-form-item label="运营状态">
                <el-select v-model="searchParams.status" clearable placeholder="全部状态">
                  <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </div>
          </el-form>

          <div class="actions-row">
            <div class="actions-group">
              <el-button v-if="canAdd" type="primary" plain @click="showAddDialog">新增</el-button>
              <el-button v-if="canEdit" type="warning" plain @click="handleEditSelected">修改</el-button>
              <el-button v-if="canDelete" type="danger" plain @click="handleDeleteSelected">删除</el-button>
              <el-button v-if="canExport" type="success" plain :loading="exporting" @click="handleExportExcel">导出</el-button>
              <el-button v-if="canImport" @click="showImportDialog">批量上传</el-button>
            </div>
            <div class="actions-group">
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </div>
          </div>
        </section>

        <section class="card-shell table-card">
          <div class="section-head table-head">
            <div>
              <h2>机构总览列表</h2>
              <p>仅展示机构基础信息，单击行可同步右侧档案面板。</p>
            </div>
            <div class="table-badges">
              <span class="badge-item">已选 {{ selectedRows.length }} 项</span>
              <span class="badge-item">页码 {{ sanatoriumStore.currentPage }}</span>
            </div>
          </div>

          <el-table
            :data="sanatoriumStore.sanatoriumList"
            v-loading="sanatoriumStore.loading"
            border
            stripe
            table-layout="fixed"
            row-key="id"
            @selection-change="handleSelectionChange"
          >
            <el-table-column v-if="canSelectRows" type="selection" width="50" align="center" :resizable="false" />
            <el-table-column prop="sanaName" label="机构名称" min-width="180" show-overflow-tooltip :resizable="false" />
            <el-table-column prop="sanaAffiliation" label="所属区划" min-width="130" show-overflow-tooltip :resizable="false" />
            <el-table-column prop="sanaAddress" label="机构地址" min-width="220" show-overflow-tooltip :resizable="false" />
            <el-table-column label="运营状态" width="120" align="center" :resizable="false">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="查看详情" width="120" align="center" :resizable="false">
              <template #default="{ row }">
                <el-button class="detail-link-btn" type="primary" size="small" @click.stop="goToDetail(row)">
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              background
              layout="total, prev, pager, next, jumper"
              :total="sanatoriumStore.total"
              :page-size="7"
              :current-page="sanatoriumStore.currentPage"
              @current-change="handlePageChange"
            />
          </div>
        </section>
      </div>

      <div class="side-column">
        <section class="card-shell profile-card" v-if="activeRecord">
          <div class="section-head profile-head">
            <div>
              <h2>机构档案面板</h2>
              <p>聚焦当前机构基础信息与资源配置水平。</p>
            </div>
            <el-tag :type="statusType(activeRecord.status)" effect="light">{{ statusText(activeRecord.status) }}</el-tag>
          </div>

          <article class="focus-panel">
            <div class="focus-line">
              <h3>{{ activeRecord.sanaName || '--' }}</h3>
              <div class="big-rate">
                <strong>{{ occupancyRate(activeRecord) }}</strong>
                <small>床位使用率</small>
              </div>
            </div>
            <p class="affiliation">{{ activeRecord.sanaAffiliation || '--' }}</p>
            <p class="address">{{ activeRecord.sanaAddress || '--' }}</p>
          </article>

          <div class="archive-grid">
            <article class="archive-item">
              <span>统一社会信用代码</span>
              <strong>{{ activeRecord.uscc || '--' }}</strong>
            </article>
            <article class="archive-item">
              <span>法人姓名</span>
              <strong>{{ activeRecord.legalPersons || '--' }}</strong>
            </article>
            <article class="archive-item">
              <span>法人联系方式</span>
              <strong>{{ activeRecord.legalPhone || '--' }}</strong>
            </article>
            <article class="archive-item">
              <span>床位总数</span>
              <strong>{{ safeNum(activeRecord.bedCount) }}</strong>
            </article>
            <article class="archive-item">
              <span>已用床位数</span>
              <strong>{{ safeNum(activeRecord.bedInUse) }}</strong>
            </article>
            <article class="archive-item">
              <span>养老人员数</span>
              <strong>{{ safeNum(activeRecord.elderCount) }}</strong>
            </article>
            <article class="archive-item">
              <span>护理人员数</span>
              <strong>{{ safeNum(activeRecord.nursingCount) }}</strong>
            </article>
            <article class="archive-item">
              <span>医护人员数</span>
              <strong>{{ safeNum(activeRecord.medicalCount) }}</strong>
            </article>
          </div>

          <section class="resource-card">
            <div class="resource-head">
              <h3>资源健康度</h3>
              <span>基于机构当前统计自动计算</span>
            </div>

            <div class="resource-row">
              <div class="resource-label">
                <span>床位占用</span>
                <strong>{{ safeNum(activeRecord.bedInUse) }} / {{ safeNum(activeRecord.bedCount) }}</strong>
              </div>
              <el-progress :percentage="toPercent(activeRecord.bedInUse, activeRecord.bedCount)" :stroke-width="10" />
            </div>

            <div class="resource-row">
              <div class="resource-label">
                <span>护理配置</span>
                <strong>{{ safeNum(activeRecord.nursingCount) }} / {{ safeNum(activeRecord.elderCount) }}</strong>
              </div>
              <el-progress
                :percentage="toPercent(activeRecord.nursingCount, activeRecord.elderCount)"
                :stroke-width="10"
                status="success"
              />
            </div>

            <div class="resource-row">
              <div class="resource-label">
                <span>医护配置</span>
                <strong>{{ safeNum(activeRecord.medicalCount) }} / {{ safeNum(activeRecord.elderCount) }}</strong>
              </div>
              <el-progress
                :percentage="toPercent(activeRecord.medicalCount, activeRecord.elderCount)"
                :stroke-width="10"
                status="warning"
              />
            </div>
          </section>
        </section>

        <section class="card-shell profile-card empty-state" v-else>
          <el-empty description="请在左侧点击“查看详情”后查看机构档案" :image-size="78" />
        </section>
      </div>
    </section>

    <el-dialog
      v-model="sanatoriumDialogVisible"
      :title="dialogType === 'add' ? '新增康养机构' : '编辑康养机构'"
      width="680px"
    >
      <el-form ref="sanatoriumForm" :model="formModel" :rules="formRules" label-width="110px">
        <el-form-item label="机构名称" prop="sanaName" required>
          <el-input v-model="formModel.sanaName" placeholder="请输入机构名称" />
        </el-form-item>
        <el-form-item label="所属区划" prop="sanaAffiliation" required>
          <el-input v-model="formModel.sanaAffiliation" placeholder="请输入所属区划" />
        </el-form-item>
        <el-form-item label="机构地址" prop="sanaAddress" required>
          <el-input v-model="formModel.sanaAddress" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="运营状态" prop="status" required>
          <el-select v-model="formModel.status" placeholder="请选择运营状态">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="统一信用代码" prop="uscc" required>
          <el-input
            v-model="formModel.uscc"
            :disabled="dialogType === 'edit'"
            placeholder="请输入统一社会信用代码"
          />
        </el-form-item>
        <el-form-item label="法人姓名" prop="legalPersons" required>
          <el-input
            v-model="formModel.legalPersons"
            :disabled="dialogType === 'edit'"
            placeholder="请输入法人姓名"
          />
        </el-form-item>
        <el-form-item label="法人电话" prop="legalPhone" required>
          <el-input
            v-model="formModel.legalPhone"
            :disabled="dialogType === 'edit'"
            placeholder="请输入法人联系方式"
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="床位总数" prop="bedCount" required>
              <el-input-number v-model="formModel.bedCount" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="已用床位数" prop="bedInUse" required>
              <el-input-number
                v-model="formModel.bedInUse"
                :min="0"
                :max="formModel.bedCount"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="养老人员数" prop="elderCount" required>
              <el-input-number v-model="formModel.elderCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="护理人员数" prop="nursingCount" required>
              <el-input-number v-model="formModel.nursingCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="医护人员数" prop="medicalCount" required>
          <el-input-number v-model="formModel.medicalCount" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="sanatoriumDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSanatorium">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="导入康养机构信息" width="520px">
      <el-upload
        drag
        action=""
        :auto-upload="false"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :show-file-list="true"
        :limit="1"
        :file-list="fileList"
        accept=".xlsx,.xls"
      >
        <el-icon class="el-icon--upload"><Upload /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或点击选择文件</div>
        <template #tip>
          <div class="el-upload__tip">仅支持 Excel 文件（.xlsx/.xls）</div>
          <div class="el-upload__tip">
            <el-link type="primary" @click="downloadTemplate">下载导入模板</el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Upload } from '@element-plus/icons-vue';
import type { SanatoriumFormData } from '@/stores/sanatoriumStore';
import { useSanatoriumStore } from '@/stores/sanatoriumStore';
import { exportToExcel } from '@/api/sanatorium';
import { useUserStore } from '@/stores/userStore';
import { getDictItems } from '@/api/dict';
import { hasResourcePath } from '@/constants/authRoles';

const sanatoriumStore = useSanatoriumStore();
const userStore = useUserStore();
const sanatoriumForm = ref<FormInstance>();

const formRules = reactive<FormRules>({
  sanaName: [{ required: true, message: '请输入机构名称', trigger: 'blur' }],
  sanaAffiliation: [{ required: true, message: '请输入所属区划', trigger: 'blur' }],
  sanaAddress: [{ required: true, message: '请输入机构地址', trigger: 'blur' }],
  status: [{ required: true, message: '请选择运营状态', trigger: 'change' }],
  uscc: [{ required: true, message: '请输入统一社会信用代码', trigger: 'blur' }],
  legalPersons: [{ required: true, message: '请输入法人姓名', trigger: 'blur' }],
  legalPhone: [{ required: true, message: '请输入法人联系方式', trigger: 'blur' }],
  bedCount: [{ required: true, message: '请输入床位总数', trigger: 'blur' }],
  bedInUse: [{ required: true, message: '请输入已用床位数', trigger: 'blur' }],
  elderCount: [{ required: true, message: '请输入养老人员数', trigger: 'blur' }],
  nursingCount: [{ required: true, message: '请输入护理人员数', trigger: 'blur' }],
  medicalCount: [{ required: true, message: '请输入医护人员数', trigger: 'blur' }]
});

const searchParams = reactive({
  sanaName: '',
  sanaAffiliation: '',
  status: undefined as number | undefined
});

const sanatoriumDialogVisible = ref(false);
const importDialogVisible = ref(false);
const dialogType = ref<'add' | 'edit'>('add');
const selectedRows = ref<any[]>([]);
const fileList = ref<any[]>([]);
const importFile = ref<File | null>(null);
const exporting = ref(false);
const importing = ref(false);
const activeRecord = ref<any | null>(null);
const statusOptions = ref<Array<{ label: string; value: number }>>([
  { label: '正常运营', value: 0 },
  { label: '停业整顿', value: 1 },
  { label: '注销取缔', value: 2 }
]);

const canAdd = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/sanatorium/add'));
const canEdit = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/sanatorium/update'));
const canDelete = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/sanatorium/delete'));
const canExport = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/sanatorium/export'));
const canImport = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/sanatorium/import'));
const canSelectRows = computed(() => canEdit.value || canDelete.value);

const formModel = reactive<SanatoriumFormData>({
  id: undefined,
  sanaName: '',
  sanaAffiliation: '',
  sanaAddress: '',
  status: 0,
  uscc: '',
  legalPersons: '',
  legalPhone: '',
  bedCount: 0,
  bedInUse: 0,
  elderCount: 0,
  nursingCount: 0,
  medicalCount: 0
});

const safeNum = (num: any) => (Number.isFinite(Number(num)) ? Number(num) : 0);

const statusText = (status: number) => statusOptions.value.find((item) => item.value === status)?.label || '未知状态';

const statusType = (status: number) => {
  if (status === 0) return 'success';
  if (status === 1) return 'warning';
  if (status === 2) return 'danger';
  return 'info';
};

const occupancyRate = (row: any) => {
  const total = safeNum(row?.bedCount);
  const used = safeNum(row?.bedInUse);
  if (total <= 0) {
    return '--';
  }
  return `${((used / total) * 100).toFixed(1)}%`;
};

const toPercent = (part: any, total: any) => {
  const partValue = safeNum(part);
  const totalValue = safeNum(total);
  if (totalValue <= 0) {
    return 0;
  }
  return Number(Math.min((partValue / totalValue) * 100, 100).toFixed(1));
};

const resetFormModel = () => {
  Object.assign(formModel, {
    id: undefined,
    sanaName: '',
    sanaAffiliation: '',
    sanaAddress: '',
    status: 0,
    uscc: '',
    legalPersons: '',
    legalPhone: '',
    bedCount: 0,
    bedInUse: 0,
    elderCount: 0,
    nursingCount: 0,
    medicalCount: 0
  });
};

const setActiveRecord = (row: any) => {
  activeRecord.value = row;
};

const goToDetail = (row: any) => {
  setActiveRecord(row);
};

watch(
  () => sanatoriumStore.sanatoriumList,
  (list) => {
    if (!list || list.length === 0) {
      activeRecord.value = null;
      return;
    }

    if (!activeRecord.value) {
      return;
    }

    const matched = list.find((item) => item.id === activeRecord.value.id);
    activeRecord.value = matched || null;
  },
  { immediate: true }
);

onMounted(() => {
  sanatoriumStore.pageSize = 7;
  sanatoriumStore.currentPage = 1;
  loadStatusOptions();
  sanatoriumStore.fetchSanatoriumPage();
});

const loadStatusOptions = async () => {
  try {
    const res: any = await getDictItems('SANATORIUM_STATUS');
    const list = Array.isArray(res?.data) ? res.data : [];
    const parsed = list
      .map((item: any) => ({
        value: Number(item?.itemValue),
        label: String(item?.itemLabel || '')
      }))
      .filter((item: any) => Number.isFinite(item.value) && item.label);
    if (parsed.length > 0) {
      statusOptions.value = parsed;
    }
  } catch {
    // 保留本地默认值兜底
  }
};

const handleSearch = () => {
  sanatoriumStore.currentPage = 1;
  sanatoriumStore.fetchSanatoriumPage(searchParams);
};

const handleReset = () => {
  Object.assign(searchParams, {
    sanaName: '',
    sanaAffiliation: '',
    status: undefined
  });
  sanatoriumStore.resetSearchParams();
  sanatoriumStore.fetchSanatoriumPage();
};

const handlePageChange = (page: number) => {
  sanatoriumStore.currentPage = page;
  sanatoriumStore.fetchSanatoriumPage();
};

const handleSelectionChange = (selection: any[]) => {
  selectedRows.value = selection;
};

const showAddDialog = () => {
  if (!canAdd.value) {
    ElMessage.warning('当前角色无新增权限');
    return;
  }
  dialogType.value = 'add';
  resetFormModel();
  sanatoriumDialogVisible.value = true;
};

const handleEditSelected = () => {
  if (!canEdit.value) {
    ElMessage.warning('当前角色无编辑权限');
    return;
  }
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要编辑的康养机构');
    return;
  }
  if (selectedRows.value.length > 1) {
    ElMessage.warning('一次只能编辑一家康养机构');
    return;
  }

  const selected = selectedRows.value[0];
  dialogType.value = 'edit';
  Object.assign(formModel, {
    id: selected.id,
    sanaName: selected.sanaName,
    sanaAffiliation: selected.sanaAffiliation,
    sanaAddress: selected.sanaAddress,
    status: selected.status,
    uscc: selected.uscc,
    legalPersons: selected.legalPersons,
    legalPhone: selected.legalPhone,
    bedCount: selected.bedCount,
    bedInUse: selected.bedInUse,
    elderCount: selected.elderCount,
    nursingCount: selected.nursingCount,
    medicalCount: selected.medicalCount
  });
  sanatoriumDialogVisible.value = true;
};

const handleDeleteSelected = async () => {
  if (!canDelete.value) {
    ElMessage.warning('当前角色无删除权限');
    return;
  }
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的康养机构');
    return;
  }

  const ids = selectedRows.value.map((row) => row.id);
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${ids.length} 家康养机构吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    await Promise.all(ids.map((id: number) => sanatoriumStore.removeSanatorium(id)));
    ElMessage.success(`成功删除 ${ids.length} 家康养机构`);
    sanatoriumStore.fetchSanatoriumPage();
  } catch {
    // 用户取消时不处理
  }
};

const saveSanatorium = async () => {
  try {
    if (dialogType.value === 'add' && !canAdd.value) {
      ElMessage.warning('当前角色无新增权限');
      return;
    }
    if (dialogType.value === 'edit' && !canEdit.value) {
      ElMessage.warning('当前角色无编辑权限');
      return;
    }

    if (!sanatoriumForm.value) {
      return;
    }

    const valid = await sanatoriumForm.value.validate();
    if (!valid) {
      return;
    }

    if (dialogType.value === 'add') {
      await sanatoriumStore.createSanatorium(formModel);
      ElMessage.success('新增成功');
    } else {
      if (!formModel.id) {
        ElMessage.error('修改失败：未获取到机构 ID');
        return;
      }
      await sanatoriumStore.updateSanatorium(formModel);
      ElMessage.success('修改成功');
    }

    sanatoriumDialogVisible.value = false;
    sanatoriumStore.fetchSanatoriumPage();
  } catch (error: any) {
    ElMessage.error(`操作失败: ${error?.message || '未知错误'}`);
  }
};

const handleExportExcel = async () => {
  if (!canExport.value) {
    ElMessage.warning('当前角色无导出权限');
    return;
  }
  exporting.value = true;
  try {
    const payload = {
      page: sanatoriumStore.currentPage,
      pageSize: sanatoriumStore.pageSize,
      sanaName: searchParams.sanaName || undefined,
      sanaAffiliation: searchParams.sanaAffiliation || undefined,
      status: searchParams.status,
      sanatoriumIds: selectedRows.value.length > 0
        ? selectedRows.value.map((row) => Number(row.id)).filter((id) => Number.isFinite(id))
        : undefined
    };
    const responseData: any = await exportToExcel(payload);
    let excelBlob: Blob | undefined;
    let responseHeaders: Record<string, any> = {};

    if (responseData?.data && responseData?.status) {
      excelBlob = responseData.data;
      responseHeaders = responseData.headers || {};
    } else if (responseData instanceof Blob) {
      excelBlob = responseData;
    }

    if (!excelBlob) {
      throw new Error('获取导出文件失败');
    }

    const url = window.URL.createObjectURL(excelBlob);
    const a = document.createElement('a');
    a.href = url;

    let fileName = '康养机构信息列表.xlsx';
    const contentDisposition = responseHeaders['content-disposition'] || responseHeaders['Content-Disposition'];
    if (contentDisposition) {
      const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
      const matches = filenameRegex.exec(contentDisposition);
      if (matches && matches[1]) {
        fileName = decodeURIComponent(matches[1].replace(/['"]/g, ''));
      }
    }

    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    }, 100);
  } catch {
    ElMessage.error('导出失败，请稍后重试');
  } finally {
    exporting.value = false;
  }
};

const showImportDialog = () => {
  if (!canImport.value) {
    ElMessage.warning('当前角色无导入权限');
    return;
  }
  importDialogVisible.value = true;
};

const downloadTemplate = async () => {
  try {
    await sanatoriumStore.exportTemplate();
  } catch {
    ElMessage.error('模板下载失败，请稍后重试');
  }
};

const handleFileChange = (file: any) => {
  importFile.value = file.raw;
  fileList.value = [file];
};

const handleFileRemove = () => {
  importFile.value = null;
  fileList.value = [];
};

const submitImport = async () => {
  if (!canImport.value) {
    ElMessage.warning('当前角色无导入权限');
    return;
  }
  if (!importFile.value) {
    ElMessage.warning('请选择文件');
    return;
  }

  importing.value = true;
  try {
    const response: any = await sanatoriumStore.importExcel(importFile.value);
    const result = response?.data;

    if (result && typeof result === 'object' && 'successCount' in result) {
      const { totalCount, successCount, failCount, failDetails } = result;

      if (failCount === 0) {
        ElMessage.success(`导入成功：共 ${successCount} 条数据全部导入`);
      } else if (successCount > 0) {
        const detailLines = Array.isArray(failDetails) && failDetails.length > 0
          ? failDetails.slice(0, 20).join('\n')
          : '';
        ElMessageBox.alert(
          `共读取 ${totalCount} 条，成功 ${successCount} 条，失败 ${failCount} 条。\n\n${detailLines}`,
          '部分导入成功',
          { type: 'warning', confirmButtonText: '知道了' }
        );
      } else {
        const detailLines = Array.isArray(failDetails) && failDetails.length > 0
          ? failDetails.slice(0, 20).join('\n')
          : '';
        ElMessageBox.alert(
          `全部 ${failCount} 条数据导入失败。\n\n${detailLines}`,
          '导入失败',
          { type: 'error', confirmButtonText: '知道了' }
        );
      }
    } else {
      ElMessage.success('导入成功');
    }

    importDialogVisible.value = false;
    fileList.value = [];
    importFile.value = null;
    sanatoriumStore.fetchSanatoriumPage();
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '导入失败';
    ElMessage.error(msg);
  } finally {
    importing.value = false;
  }
};
</script>

<style scoped>
.sana-page {
  --s-bg: #f3f6fa;
  --s-panel: #ffffff;
  --s-border: #d7e0ea;
  --s-line: #e7edf4;
  --s-title: #1a2f4d;
  --s-text: #5d728f;
  --s-strong: #2f5e9e;
  --s-shadow: 0 2px 8px rgba(26, 47, 77, 0.06);

  min-height: 0;
  padding: 16px 18px 8px;
  box-sizing: border-box;
  background: var(--s-bg);
}

.card-shell {
  background: var(--s-panel);
  border: 1px solid var(--s-border);
  border-radius: 8px;
  box-shadow: var(--s-shadow);
}

.top-banner {
  display: block;
  padding: 12px 16px;
}

.banner-kicker {
  margin: 0;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-weight: 700;
  color: #5d769d;
}

.banner-title-row {
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.banner-main h1 {
  margin: 0;
  color: var(--s-title);
  font-size: 24px;
  line-height: 1.2;
  font-weight: 700;
}

.banner-chip {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #3b6fb1;
  border: 1px solid #d9e1ea;
  background: #f8fafc;
}

.banner-desc {
  margin: 6px 0 0;
  color: var(--s-text);
  line-height: 1.5;
  font-size: 12px;
  max-width: 860px;
}

.workbench-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(350px, 1fr);
  gap: 14px;
  align-items: start;
}

.main-column,
.side-column {
  min-width: 0;
}

.side-column {
  align-self: start;
}

.filter-card,
.table-card,
.profile-card {
  padding: 16px;
}

.filter-card {
  padding: 12px 14px;
}

.filter-card .section-head {
  margin-bottom: 8px;
}

.filter-card .section-head h2 {
  font-size: 17px;
  line-height: 1.2;
}

.filter-card .section-head p {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
}

.table-card {
  margin-top: 14px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.section-head h2 {
  margin: 0;
  color: var(--s-title);
  font-size: 19px;
}

.section-head p {
  margin: 6px 0 0;
  color: var(--s-text);
  font-size: 12px;
  line-height: 1.6;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 12px;
}

.filter-card .filters-grid {
  gap: 8px 10px;
}

.filters-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-card :deep(.el-form-item__label) {
  padding-bottom: 4px;
  line-height: 1.2;
}

.filter-card :deep(.el-input__wrapper),
.filter-card :deep(.el-select__wrapper) {
  min-height: 34px;
}

.actions-row {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--s-line);
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 10px;
}

.filter-card .actions-row {
  margin-top: 10px;
  padding-top: 10px;
}

.actions-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-card .actions-group :deep(.el-button) {
  height: 32px;
  padding: 0 12px;
}

.filter-card .actions-group :deep(.el-button--primary),
.filter-card .actions-group :deep(.el-button--primary.is-plain) {
  background: #2f5e9e;
  border-color: #2f5e9e;
  color: #ffffff;
}

.filter-card .actions-group :deep(.el-button--primary:hover),
.filter-card .actions-group :deep(.el-button--primary:focus),
.filter-card .actions-group :deep(.el-button--primary.is-plain:hover),
.filter-card .actions-group :deep(.el-button--primary.is-plain:focus) {
  background: #254f87;
  border-color: #254f87;
  color: #ffffff;
}

.filter-card .actions-group :deep(.el-button--primary:active),
.filter-card .actions-group :deep(.el-button--primary.is-plain:active) {
  background: #1f4577;
  border-color: #1f4577;
  color: #ffffff;
}

.table-head {
  margin-bottom: 10px;
}

.table-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.badge-item {
  border-radius: 4px;
  border: 1px solid #d9e1ea;
  background: #f8fafc;
  color: #5d728f;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 10px;
}

.pagination-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.detail-link-btn {
  min-width: 76px;
  height: 28px;
  padding: 0 10px;
  border-radius: 4px;
  border-color: #3b6fb1;
  background: #3b6fb1;
  color: #ffffff;
  font-weight: 600;
}

.detail-link-btn:hover,
.detail-link-btn:focus {
  border-color: #2f5e9e;
  background: #2f5e9e;
  color: #ffffff;
}

.detail-link-btn:active {
  border-color: #254f87;
  background: #254f87;
  color: #ffffff;
}

.profile-card {
  position: static;
  width: 100%;
  height: auto;
  box-sizing: border-box;
}

.side-column > .profile-card:not(.empty-state) {
  max-height: none;
  overflow: visible;
}

.focus-panel {
  border: 1px solid #d9e1ea;
  border-radius: 8px;
  padding: 14px;
  background: #f8fafc;
}

.focus-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.focus-line h3 {
  margin: 0;
  font-size: 22px;
  color: var(--s-title);
  line-height: 1.25;
}

.big-rate {
  text-align: right;
  min-width: 100px;
}

.big-rate strong {
  display: block;
  color: var(--s-strong);
  font-size: 22px;
  line-height: 1;
}

.big-rate small {
  display: block;
  margin-top: 6px;
  color: #6f86a4;
  font-size: 12px;
}

.affiliation {
  margin: 8px 0 0;
  color: #5d728f;
  font-size: 13px;
}

.address {
  margin: 6px 0 0;
  color: #4f6687;
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
}

.archive-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.archive-item {
  border: 1px solid #d9e1ea;
  border-radius: 8px;
  padding: 10px 12px;
  min-width: 0;
  background: #f8fafc;
}

.archive-item span {
  display: block;
  color: #5d728f;
  font-size: 12px;
}

.archive-item strong {
  display: block;
  margin-top: 6px;
  color: #1a2f4d;
  font-size: 14px;
  line-height: 1.45;
  word-break: break-word;
}

.resource-card {
  margin-top: 12px;
  border-radius: 8px;
  border: 1px solid #d9e1ea;
  background: #ffffff;
  padding: 12px;
}

.resource-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
}

.resource-head h3 {
  margin: 0;
  color: var(--s-title);
  font-size: 17px;
}

.resource-head span {
  color: var(--s-text);
  font-size: 12px;
}

.resource-row {
  margin-top: 10px;
}

.resource-label {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 7px;
}

.resource-label span {
  color: #5d728f;
  font-size: 13px;
}

.resource-label strong {
  color: #1a2f4d;
  font-size: 13px;
}

.empty-state {
  min-height: 168px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 20px;
  position: static;
}

.upload-box {
  display: flex;
  justify-content: center;
}

:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-tag) {
  border-radius: 4px;
}

:deep(.el-table__header th) {
  background: #f8fafc;
  color: #5d728f;
  font-weight: 700;
}

:deep(.el-table td.el-table__cell),
:deep(.el-table th.el-table__cell) {
  padding: 11px 0;
}

:deep(.el-table .current-row > td.el-table__cell) {
  background: #edf3fb !important;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  box-shadow: none;
  border: 1px solid #d5e0ee;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused) {
  border-color: #3b6fb1;
  box-shadow: 0 0 0 3px rgba(59, 111, 177, 0.12);
}

:deep(.el-button--primary) {
  background: #3b6fb1;
  border-color: #3b6fb1;
}

@media (max-width: 1380px) {
  .workbench-grid {
    grid-template-columns: 1fr;
  }

  .profile-card {
    position: static;
  }
}

@media (max-width: 980px) {
  .sana-page {
    padding: 12px;
  }

  .banner-main h1 {
    font-size: 21px;
  }

  .filters-grid {
    grid-template-columns: 1fr;
  }

  .archive-grid {
    grid-template-columns: 1fr;
  }

  .pagination-wrap {
    justify-content: center;
  }

}

@media (max-width: 640px) {
  .top-banner {
    padding: 10px 12px;
  }

  .filter-card,
  .table-card,
  .profile-card {
    padding: 12px;
  }

  .section-head,
  .actions-row,
  .focus-line,
  .resource-head,
  .resource-label {
    flex-direction: column;
    align-items: flex-start;
  }

  .big-rate {
    text-align: left;
  }

}
</style>
