<template>
  <!-- 老人档案页：查询、表格、编辑弹窗和导入导出都集中在此页面。 -->
  <div class="elder-page">
    <section class="card-shell page-header">
      <div>
        <h1>康养用户信息管理</h1>
        <p>统一维护老人档案、入住状态与护理画像，支持批量导入导出。</p>
      </div>
      <div class="head-stats">
        <span class="chip">总数 {{ total }}</span>
        <span class="chip active">已选 {{ selectedIds.length }}</span>
      </div>
    </section>

    <section class="card-shell filter-panel">
      <el-form :model="queryForm" label-position="top">
        <div class="filter-grid">
          <el-form-item label="老人姓名">
            <el-input v-model.trim="queryForm.elderName" clearable placeholder="请输入老人姓名" @keyup.enter="onSearch" />
          </el-form-item>

          <el-form-item label="占用床位类型">
            <el-select v-model="queryForm.occupiedBedType" clearable placeholder="全部床位类型">
              <el-option v-for="item in bedTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>

          <el-form-item label="房间号">
            <el-input v-model.trim="queryForm.roomNumber" clearable placeholder="请输入房间号" @keyup.enter="onSearch" />
          </el-form-item>

          <el-form-item label="入院时间">
            <el-date-picker
              v-model="queryForm.inpatientsTime"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择入院时间"
              clearable
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="自理能力">
            <el-select v-model="queryForm.selfCare" clearable placeholder="全部能力等级">
              <el-option v-for="item in selfCareOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>

          <el-form-item label="入住类型">
            <el-select v-model="queryForm.familySituation" clearable placeholder="全部入住类型">
              <el-option v-for="item in familySituationOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>

          <el-form-item label="所属机构">
            <el-select
              v-model="queryForm.sanaId"
              clearable
              placeholder="请选择机构"
              fit-input-width
            >
              <el-option v-for="item in sanaOptions" :key="item.id" :label="item.sanaName" :value="item.id" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>

      <div class="toolbar">
        <div class="group">
          <el-button v-if="canAddElder" type="primary" :icon="Plus" @click="openAddDialog">新增</el-button>
          <el-button v-if="canDeleteElder" type="danger" plain :icon="Delete" :disabled="selectedIds.length === 0" @click="deleteSelected">删除已选</el-button>
          <el-button v-if="canImportElder" :icon="Upload" @click="openImportDialog">导入</el-button>
          <el-button v-if="canExportElder" :icon="Download" @click="handleExport">导出</el-button>
        </div>
        <div class="group">
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </div>
      </div>
    </section>

    <section class="card-shell table-panel" v-loading="loading">
      <el-table
        :data="elderList"
        border
        stripe
        table-layout="fixed"
        row-key="id"
        @selection-change="onSelectionChange"
      >
        <el-table-column v-if="canDeleteElder || canExportElder" type="selection" width="50" align="center" :resizable="false" />

        <el-table-column label="老人信息" min-width="180" :resizable="false">
          <template #default="{ row }">
            <div class="cell-stack">
              <strong>{{ row.elderName || '--' }}</strong>
              <span>{{ sexText(row.sex) }} / {{ row.age ?? '--' }} 岁</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="sanaName" label="所属机构" min-width="220" show-overflow-tooltip :resizable="false" />

        <el-table-column label="入住画像" min-width="180" :resizable="false">
          <template #default="{ row }">
            <div class="cell-stack">
              <span>入住类型：{{ familySituationText(row.familySituation) }}</span>
              <span>自理能力：{{ selfCareText(row.selfCare) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="床位与费用" min-width="160" :resizable="false">
          <template #default="{ row }">
            <div class="cell-stack">
              <span>床位：{{ bedTypeText(row.occupiedBedType) }}</span>
              <span>房间：{{ row.roomNumber || '--' }}</span>
              <span>收费：{{ row.fees ?? '--' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="在住时间" min-width="190" :resizable="false">
          <template #default="{ row }">
            <div class="cell-stack">
              <span>入院：{{ row.inpatientsTime || '--' }}</span>
              <span>出院：{{ row.outpatientsTime || '--' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center" :resizable="false">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" effect="light">
              {{ row.status === 0 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="190" align="center" :resizable="false">
          <template #default="{ row }">
            <el-button v-if="canUpdateElder" text type="primary" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button text type="primary" :icon="Paperclip" @click="openAttachmentDialog(row)">附件</el-button>
            <el-button v-if="canDeleteElder" text type="danger" :icon="Delete" @click="deleteOne(Number(row.id))">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && elderList.length === 0" description="暂无老人数据" :image-size="86" />

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="queryForm.page"
          :page-size="queryForm.pageSize"
          :page-sizes="[8, 12, 20, 30]"
          @size-change="onPageSizeChange"
          @current-change="onPageChange"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialog.form"
      class="elder-form-dialog"
      :title="isEdit ? '编辑老人信息' : '新增老人信息'"
      width="760px"
    >
      <el-form ref="elderFormRef" :model="formModel" :rules="formRules" label-width="110px">
        <el-row :gutter="12">
          <el-col :xs="24" :md="12">
            <el-form-item label="所属机构" prop="sanaId">
              <el-select
                v-model="formModel.sanaId"
                placeholder="请选择机构"
                style="width: 100%"
                fit-input-width
              >
                <el-option v-for="item in sanaOptions" :key="item.id" :label="item.sanaName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="老人姓名" prop="elderName">
              <el-input v-model.trim="formModel.elderName" />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-form-item label="性别" prop="sex">
              <el-select v-model="formModel.sex" style="width: 100%">
                <el-option v-for="item in sexOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="formModel.age" :min="0" :max="130" style="width: 100%" />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-form-item label="身份证号" prop="idNumber">
              <el-input
                :model-value="displayIdNumber"
                placeholder="请输入身份证号"
                @focus="onIdNumberFocus"
                @blur="onIdNumberBlur"
                @update:model-value="onIdNumberChange"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="手机号" prop="phoneNumber">
              <el-input v-model.trim="formModel.phoneNumber" />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-form-item label="入住类型" prop="familySituation">
              <el-select v-model="formModel.familySituation" style="width: 100%">
                <el-option v-for="item in familySituationOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="自理能力" prop="selfCare">
              <el-select v-model="formModel.selfCare" style="width: 100%">
                <el-option v-for="item in selfCareOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-form-item label="占用床位类型" prop="occupiedBedType">
              <el-select v-model="formModel.occupiedBedType" style="width: 100%">
                <el-option v-for="item in bedTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="房间号" prop="roomNumber">
              <el-input v-model.trim="formModel.roomNumber" maxlength="30" placeholder="如 3F-301" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="收费标准" prop="fees">
              <el-input-number v-model="formModel.fees" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-form-item label="监护人姓名" prop="guardianName">
              <el-input v-model.trim="formModel.guardianName" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="监护人电话" prop="guardianPhone">
              <el-input v-model.trim="formModel.guardianPhone" />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-form-item label="入院时间" prop="inpatientsTime">
              <el-date-picker v-model="formModel.inpatientsTime" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="出院时间" prop="outpatientsTime">
              <el-date-picker v-model="formModel.outpatientsTime" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="家庭住址" prop="homeAddress">
              <el-input v-model.trim="formModel.homeAddress" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialog.form = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialog.import" title="导入老人信息" width="520px">
      <el-upload
        drag
        action=""
        :auto-upload="false"
        :show-file-list="true"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="onImportFileChange"
        :on-remove="onImportFileRemove"
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
        <el-button @click="dialog.import = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dialog.attachments"
      class="elder-attachment-dialog"
      :title="`${currentAttachmentElder?.elderName || '老人'} - 图片与附件`"
      width="860px"
    >
      <div v-if="canUploadAttachment" class="attachment-upload-bar">
        <el-upload
          action=""
          :auto-upload="false"
          :show-file-list="false"
          accept="image/*"
          :on-change="(file: UploadFile) => uploadAttachmentFile(file, 0)"
        >
          <el-button type="primary" :icon="Picture" :loading="attachmentUploading">上传图片</el-button>
        </el-upload>
        <el-upload
          action=""
          :auto-upload="false"
          :show-file-list="false"
          :on-change="(file: UploadFile) => uploadAttachmentFile(file, 1)"
        >
          <el-button :icon="UploadFilled" :loading="attachmentUploading">上传附件</el-button>
        </el-upload>
      </div>

      <div class="attachment-section" v-loading="attachmentLoading">
        <div class="section-title">档案图片</div>
        <div v-if="imageAttachments.length > 0" class="image-grid">
          <div v-for="item in imageAttachments" :key="item.id" class="image-card">
            <el-image
              class="elder-image"
              :src="buildImageProxySrc(item.fileUrl)"
              :preview-src-list="imagePreviewList"
              fit="cover"
              preview-teleported
            />
            <div class="attachment-name" :title="item.fileName">{{ item.fileName || '图片' }}</div>
            <el-button v-if="canDeleteAttachment" text type="danger" size="small" @click="removeAttachment(Number(item.id))">删除</el-button>
          </div>
        </div>
        <el-empty v-else description="暂无图片" :image-size="72" />
      </div>

      <div class="attachment-section">
        <div class="section-title">普通附件</div>
        <el-table :data="fileAttachments" border stripe empty-text="暂无附件">
          <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip />
          <el-table-column label="大小" width="110" align="center">
            <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="上传时间" width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <el-button text type="primary" @click="downloadAttachment(row)">下载</el-button>
              <el-button v-if="canDeleteAttachment" text type="danger" @click="removeAttachment(Number(row.id))">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile } from 'element-plus';
import { Plus, Delete, Upload, Download, Edit, Paperclip, Picture, UploadFilled } from '@element-plus/icons-vue';
import { getDictItemsBatch } from '@/api/dict';
import { useUserStore } from '@/stores/userStore';
import { hasResourcePath } from '@/constants/authRoles';
import {
  getElderPage,
  addElder,
  updateElder,
  deleteElder,
  exportElderExcel,
  importElderExcel,
  getElderAttachments,
  uploadElderAttachment,
  deleteElderAttachment,
  type ElderAttachmentRecord,
  getSanatoriumPage as pageSanatoriumAPI
} from '@/api/elder';
import { buildImageProxySrc, downloadFileByUrl } from '@/api/file';

// 所属机构下拉框的轻量结构。
interface SanaOption {
  id: number;
  sanaName: string;
}

// 页面内部使用的老人数据结构，兼容后端字段差异和表单编辑场景。
interface ElderItem {
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
  occupied_bed_type?: number;
  roomNumber?: string;
  room_number?: string;
  guardianName?: string;
  guardianPhone?: string;
  selfCare?: number;
  inpatientsTime?: string;
  outpatientsTime?: string;
  fees?: number;
  status?: number;
}

// 字典项统一使用数字 value，避免字符串数字比较导致的匹配偏差。
interface DictOption {
  label: string;
  value: number;
}

// 后端字典不可用时的兜底选项，保证页面可渲染、可录入。
const defaultSexOptions: DictOption[] = [
  { label: '男', value: 0 },
  { label: '女', value: 1 },
  { label: '未知', value: 2 }
];

const defaultSelfCareOptions: DictOption[] = [
  { label: '能力完好', value: 0 },
  { label: '轻度失能', value: 1 },
  { label: '中度失能', value: 2 },
  { label: '重度失能', value: 3 },
  { label: '完全失能', value: 4 }
];

const defaultFamilySituationOptions: DictOption[] = [
  { label: '社会老年人', value: 0 },
  { label: '低保', value: 1 },
  { label: '特困老年人', value: 2 },
  { label: '建档立卡', value: 3 }
];

const defaultBedTypeOptions: DictOption[] = [
  { label: '普通床位', value: 0 },
  { label: '护理型床位', value: 1 }
];

const sexOptions = ref<DictOption[]>([...defaultSexOptions]);
const selfCareOptions = ref<DictOption[]>([...defaultSelfCareOptions]);
const familySituationOptions = ref<DictOption[]>([...defaultFamilySituationOptions]);
const bedTypeOptions = ref<DictOption[]>([...defaultBedTypeOptions]);

// 查询条件和分页状态。
const queryForm = reactive({
  elderName: '',
  occupiedBedType: undefined as number | undefined,
  roomNumber: '',
  inpatientsTime: '',
  selfCare: undefined as number | undefined,
  familySituation: undefined as number | undefined,
  sanaId: undefined as number | undefined,
  page: 1,
  pageSize: 12
});

const dialog = reactive({ form: false, import: false, attachments: false });
const userStore = useUserStore();
const loading = ref(false);
const saving = ref(false);
const importing = ref(false);
const attachmentLoading = ref(false);
const attachmentUploading = ref(false);
const isEdit = ref(false);
const importFile = ref<File | null>(null);
const idNumberEditing = ref(false);
const idNumberChanged = ref(false);
const maskedIdNumber = ref('');

const elderFormRef = ref<FormInstance>();
const elderList = ref<ElderItem[]>([]);
const total = ref(0);
const sanaOptions = ref<SanaOption[]>([]);
const selectedRows = ref<ElderItem[]>([]);
const selectedIds = ref<number[]>([]);
const currentAttachmentElder = ref<ElderItem | null>(null);
const attachmentList = ref<ElderAttachmentRecord[]>([]);

// 表单模型同时承载新增和编辑。
const formModel = reactive<ElderItem>({
  id: undefined,
  sanaId: undefined,
  elderName: '',
  sex: 0,
  age: 60,
  idNumber: '',
  phoneNumber: '',
  homeAddress: '',
  familySituation: 0,
  occupiedBedType: 0,
  roomNumber: '',
  guardianName: '',
  guardianPhone: '',
  selfCare: 0,
  inpatientsTime: '',
  outpatientsTime: '',
  fees: 0,
  status: 0
});

const formRules = reactive<FormRules>({
  sanaId: [{ required: true, message: '请选择所属机构', trigger: 'change' }],
  elderName: [{ required: true, message: '请输入老人姓名', trigger: 'blur' }],
  sex: [{ required: true, message: '请选择性别', trigger: 'change' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'change' }],
  familySituation: [{ required: true, message: '请选择入住类型', trigger: 'change' }],
  selfCare: [{ required: true, message: '请选择自理能力', trigger: 'change' }],
  occupiedBedType: [{ required: true, message: '请选择床位类型', trigger: 'change' }],
  inpatientsTime: [{ required: true, message: '请选择入院时间', trigger: 'change' }]
});

// 后端字典值统一转成数字，避免 "0" 和 0 混用。
const toNumberOption = (item: any): DictOption | null => {
  const rawValue = item?.itemValue;
  const numericValue = Number(rawValue);
  if (!Number.isFinite(numericValue)) {
    return null;
  }
  return {
    value: numericValue,
    label: String(item?.itemLabel || '')
  };
};

const sexText = (value?: number) => sexOptions.value.find((x) => x.value === value)?.label || '未知';

const selfCareText = (value?: number) => selfCareOptions.value.find((x) => x.value === value)?.label || '未知';
const familySituationText = (value?: number) => familySituationOptions.value.find((x) => x.value === value)?.label || '未知';

// 兼容后端历史字段 occupied_bed_type 和当前字段 occupiedBedType。
const normalizeOccupiedBedType = (value: unknown): number => {
  if (value === null || value === undefined || value === '') {
    return 0;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : 0;
};

const bedTypeText = (value?: unknown) => {
  const normalizedValue = normalizeOccupiedBedType(value);
  return bedTypeOptions.value.find((x) => x.value === normalizedValue)?.label || '普通床位';
};

const imageAttachments = computed(() => attachmentList.value.filter((item) => item.attachmentType === 0));
const fileAttachments = computed(() => attachmentList.value.filter((item) => item.attachmentType !== 0));
const imagePreviewList = computed(() => imageAttachments.value.map((item) => buildImageProxySrc(item.fileUrl)).filter(Boolean));
const canAddElder = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/elder/add'));
const canUpdateElder = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/elder/update'));
const canDeleteElder = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/elder/delete'));
const canExportElder = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/elder/export'));
const canImportElder = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/elder/import'));
const canUploadAttachment = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/elder/attachments/upload'));
const canDeleteAttachment = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/elder/attachments/delete'));

// 列表行统一归一化后再渲染，避免模板里反复兼容字段差异。
const normalizeElderRow = (item: ElderItem): ElderItem => {
  const occupiedBedType = normalizeOccupiedBedType(item.occupiedBedType ?? item.occupied_bed_type);
  return {
    ...item,
    occupiedBedType,
    roomNumber: item.roomNumber ?? item.room_number ?? ''
  };
};

// 编辑时身份证号默认脱敏展示，只有用户主动输入才覆盖原值。
const maskIdNumber = (value?: string) => {
  if (!value) {
    return '';
  }
  const normalized = value.trim();
  if (normalized.length <= 7) {
    return normalized;
  }
  return `${normalized.slice(0, 6)}${'*'.repeat(Math.max(1, normalized.length - 10))}${normalized.slice(-4)}`;
};

const displayIdNumber = computed(() => {
  if (!isEdit.value) {
    return formModel.idNumber || '';
  }
  if (idNumberEditing.value || idNumberChanged.value) {
    return idNumberChanged.value ? (formModel.idNumber || '') : '';
  }
  if (!maskedIdNumber.value) {
    return '';
  }
  return maskedIdNumber.value;
});

const onIdNumberFocus = () => {
  idNumberEditing.value = true;
  if (isEdit.value && !idNumberChanged.value) {
    formModel.idNumber = '';
  }
};

const onIdNumberBlur = () => {
  if (isEdit.value && !idNumberChanged.value) {
    formModel.idNumber = '';
  }
  idNumberEditing.value = false;
};

const onIdNumberChange = (value: string) => {
  formModel.idNumber = value.trim();
  if (isEdit.value) {
    idNumberChanged.value = true;
  }
};

const buildPagePayload = () => ({
  page: queryForm.page,
  pageSize: queryForm.pageSize,
  elderName: queryForm.elderName || undefined,
  occupiedBedType: queryForm.occupiedBedType === undefined ? undefined : Number(queryForm.occupiedBedType),
  roomNumber: queryForm.roomNumber || undefined,
  inpatientsTime: queryForm.inpatientsTime || undefined,
  selfCare: queryForm.selfCare,
  familySituation: queryForm.familySituation,
  sanaId: queryForm.sanaId
});

// 查询当前筛选条件下的老人列表。
const fetchElders = async () => {
  loading.value = true;
  try {
    const res: any = await getElderPage(buildPagePayload());
    const pageData = res?.data || {};
    elderList.value = Array.isArray(pageData.records) ? pageData.records.map((item: ElderItem) => normalizeElderRow(item)) : [];
    total.value = Number(pageData.total || 0);
    selectedRows.value = [];
    selectedIds.value = [];
  } catch {
    elderList.value = [];
    total.value = 0;
    selectedRows.value = [];
    selectedIds.value = [];
  } finally {
    loading.value = false;
  }
};

// 拉取机构下拉选项。
const fetchSanatoriumOptions = async () => {
  try {
    const res: any = await pageSanatoriumAPI({ page: 1, pageSize: 500 });
    const records = res?.data?.records || [];
    sanaOptions.value = records.map((item: any) => ({ id: Number(item.id), sanaName: String(item.sanaName || '') }));
  } catch {
    sanaOptions.value = [];
  }
};

// 批量加载字典项，优先使用后台配置，失败时退回默认值。
const loadDictOptions = async () => {
  try {
    const res: any = await getDictItemsBatch([
      'ELDER_SEX',
      'ELDER_SELF_CARE',
      'ELDER_FAMILY_SITUATION',
      'ELDER_BED_TYPE'
    ]);
    const dictMap = res?.data || {};

    const sexList = Array.isArray(dictMap.ELDER_SEX) ? dictMap.ELDER_SEX.map(toNumberOption).filter(Boolean) : [];
    const selfCareList = Array.isArray(dictMap.ELDER_SELF_CARE) ? dictMap.ELDER_SELF_CARE.map(toNumberOption).filter(Boolean) : [];
    const familyList = Array.isArray(dictMap.ELDER_FAMILY_SITUATION)
      ? dictMap.ELDER_FAMILY_SITUATION.map(toNumberOption).filter(Boolean)
      : [];
    const bedTypeList = Array.isArray(dictMap.ELDER_BED_TYPE) ? dictMap.ELDER_BED_TYPE.map(toNumberOption).filter(Boolean) : [];

    sexOptions.value = sexList.length > 0 ? (sexList as DictOption[]) : [...defaultSexOptions];
    selfCareOptions.value = selfCareList.length > 0 ? (selfCareList as DictOption[]) : [...defaultSelfCareOptions];
    familySituationOptions.value = familyList.length > 0 ? (familyList as DictOption[]) : [...defaultFamilySituationOptions];
    bedTypeOptions.value = bedTypeList.length > 0 ? (bedTypeList as DictOption[]) : [...defaultBedTypeOptions];
  } catch {
    sexOptions.value = [...defaultSexOptions];
    selfCareOptions.value = [...defaultSelfCareOptions];
    familySituationOptions.value = [...defaultFamilySituationOptions];
    bedTypeOptions.value = [...defaultBedTypeOptions];
  }
};

// 重置表单到新增初始状态。
const resetForm = () => {
  Object.assign(formModel, {
    id: undefined,
    sanaId: undefined,
    elderName: '',
    sex: 0,
    age: 60,
    idNumber: '',
    phoneNumber: '',
    homeAddress: '',
    familySituation: 0,
    occupiedBedType: 0,
    roomNumber: '',
    guardianName: '',
    guardianPhone: '',
    selfCare: 0,
    inpatientsTime: '',
    outpatientsTime: '',
    fees: 0,
    status: 0
  });
};

const onSelectionChange = (rows: ElderItem[]) => {
  selectedRows.value = rows;
  selectedIds.value = rows.map((row) => Number(row.id)).filter((id) => Number.isFinite(id));
};

const openAddDialog = () => {
  if (!canAddElder.value) {
    ElMessage.warning('当前账号无新增老人权限');
    return;
  }
  // 新增时身份证号直接明文录入，不做脱敏占位。
  isEdit.value = false;
  idNumberEditing.value = true;
  idNumberChanged.value = false;
  maskedIdNumber.value = '';
  resetForm();
  dialog.form = true;
};

const openEditDialog = (row: ElderItem) => {
  if (!canUpdateElder.value) {
    ElMessage.warning('当前账号无编辑老人权限');
    return;
  }
  // 编辑时保留原始身份证号在后台，前端仅显示脱敏值，除非用户重新输入。
  isEdit.value = true;
  idNumberEditing.value = false;
  idNumberChanged.value = false;
  resetForm();
  Object.assign(formModel, row);
  maskedIdNumber.value = row.idNumber ? maskIdNumber(row.idNumber) : '';
  formModel.idNumber = '';
  if (formModel.occupiedBedType == null) {
    formModel.occupiedBedType = 0;
  }
  if (!formModel.sanaId && row.sanaName) {
    const matched = sanaOptions.value.find((item) => item.sanaName === row.sanaName);
    if (matched) {
      formModel.sanaId = matched.id;
    }
  }
  dialog.form = true;
};

// 保存时只在编辑且用户改过身份证号时才回传该字段，避免把脱敏值误提交。
const submitForm = async () => {
  if (!isEdit.value && !canAddElder.value) {
    ElMessage.warning('当前账号无新增老人权限');
    return;
  }
  if (isEdit.value && !canUpdateElder.value) {
    ElMessage.warning('当前账号无编辑老人权限');
    return;
  }
  if (!elderFormRef.value) {
    return;
  }

  const valid = await elderFormRef.value.validate().catch(() => false);
  if (!valid) {
    return;
  }

  saving.value = true;
  try {
    const payload: ElderItem = {
      ...formModel,
      inpatientsTime: formModel.inpatientsTime || undefined,
      outpatientsTime: formModel.outpatientsTime || undefined
    };
    if (isEdit.value && !idNumberChanged.value) {
      delete payload.idNumber;
    }
    if (isEdit.value) {
      await updateElder(payload);
      ElMessage.success('修改成功');
    } else {
      await addElder(payload);
      ElMessage.success('新增成功');
    }
    dialog.form = false;
    await fetchElders();
  } finally {
    saving.value = false;
  }
};

const deleteOne = async (id: number) => {
  if (!canDeleteElder.value) {
    ElMessage.warning('当前账号无删除老人权限');
    return;
  }
  await ElMessageBox.confirm('确认删除该老人信息？', '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  });
  await deleteElder(id);
  ElMessage.success('删除成功');
  await fetchElders();
};

// 批量删除沿用单条删除接口，保证后端行为一致。
const deleteSelected = async () => {
  if (!canDeleteElder.value) {
    ElMessage.warning('当前账号无删除老人权限');
    return;
  }
  if (selectedIds.value.length === 0) {
    return;
  }

  await ElMessageBox.confirm(`确认删除已选 ${selectedIds.value.length} 条老人信息？`, '批量删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  });

  await Promise.all(selectedIds.value.map((id) => deleteElder(id)));
  ElMessage.success('批量删除成功');
  await fetchElders();
};

const onSearch = async () => {
  queryForm.page = 1;
  await fetchElders();
};

const onReset = async () => {
  queryForm.elderName = '';
  queryForm.occupiedBedType = undefined;
  queryForm.roomNumber = '';
  queryForm.inpatientsTime = '';
  queryForm.selfCare = undefined;
  queryForm.familySituation = undefined;
  queryForm.sanaId = undefined;
  queryForm.page = 1;
  queryForm.pageSize = 12;
  await fetchElders();
};

const onPageChange = async (page: number) => {
  queryForm.page = page;
  await fetchElders();
};

const onPageSizeChange = async (size: number) => {
  queryForm.pageSize = size;
  queryForm.page = 1;
  await fetchElders();
};

const handleExport = async () => {
  if (!canExportElder.value) {
    ElMessage.warning('当前账号无导出老人权限');
    return;
  }
  // 若当前勾选了老人，则导出勾选项；否则导出当前筛选结果。
  const payload = {
    ...buildPagePayload(),
    elderIds: selectedIds.value.length > 0 ? selectedIds.value : undefined
  };
  const res: any = await exportElderExcel(payload);
  const blob = res?.data instanceof Blob ? res.data : res instanceof Blob ? res : new Blob([res]);
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = '老人信息列表.xlsx';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};

const onImportFileChange = (file: UploadFile) => {
  importFile.value = (file.raw as File) || null;
};

const onImportFileRemove = () => {
  importFile.value = null;
};

const fetchAttachments = async () => {
  if (!currentAttachmentElder.value?.id) {
    attachmentList.value = [];
    return;
  }
  attachmentLoading.value = true;
  try {
    const res: any = await getElderAttachments(Number(currentAttachmentElder.value.id));
    attachmentList.value = Array.isArray(res?.data) ? res.data : [];
  } finally {
    attachmentLoading.value = false;
  }
};

const openAttachmentDialog = async (row: ElderItem) => {
  if (!row.id) {
    ElMessage.warning('请先保存老人档案');
    return;
  }
  currentAttachmentElder.value = row;
  attachmentList.value = [];
  dialog.attachments = true;
  await fetchAttachments();
};

const uploadAttachmentFile = async (uploadFile: UploadFile, attachmentType: number) => {
  if (!canUploadAttachment.value) {
    ElMessage.warning('当前账号无附件上传权限');
    return;
  }
  if (!currentAttachmentElder.value?.id) {
    ElMessage.warning('请先选择老人档案');
    return;
  }
  const rawFile = uploadFile.raw as File | undefined;
  if (!rawFile) {
    ElMessage.warning('请选择文件');
    return;
  }
  attachmentUploading.value = true;
  try {
    await uploadElderAttachment(Number(currentAttachmentElder.value.id), rawFile, attachmentType);
    ElMessage.success(attachmentType === 0 ? '图片上传成功' : '附件上传成功');
    await fetchAttachments();
  } finally {
    attachmentUploading.value = false;
  }
};

const removeAttachment = async (id: number) => {
  if (!canDeleteAttachment.value) {
    ElMessage.warning('当前账号无附件删除权限');
    return;
  }
  await ElMessageBox.confirm('确认删除该附件？', '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  });
  await deleteElderAttachment(id);
  ElMessage.success('删除成功');
  await fetchAttachments();
};

const downloadAttachment = async (row: ElderAttachmentRecord) => {
  if (!row.fileUrl) {
    ElMessage.warning('附件地址为空');
    return;
  }
  await downloadFileByUrl(row.fileUrl);
};

const formatFileSize = (size?: number) => {
  const value = Number(size || 0);
  if (!value) {
    return '--';
  }
  if (value < 1024) {
    return `${value} B`;
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
};

const submitImport = async () => {
  if (!canImportElder.value) {
    ElMessage.warning('当前账号无导入老人权限');
    return;
  }
  if (!importFile.value) {
    ElMessage.warning('请先选择导入文件');
    return;
  }
  importing.value = true;
  try {
    await importElderExcel(importFile.value);
    ElMessage.success('导入成功');
    dialog.import = false;
    importFile.value = null;
    await fetchElders();
  } finally {
    importing.value = false;
  }
};

const openImportDialog = () => {
  if (!canImportElder.value) {
    ElMessage.warning('当前账号无导入老人权限');
    return;
  }
  dialog.import = true;
};

const downloadTemplate = () => {
  // 导入模板直接从静态资源目录分发。
  const link = document.createElement('a');
  link.href = new URL('@/assets/file/老人信息模板.xlsx', import.meta.url).href;
  link.download = '老人信息模板.xlsx';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

onMounted(async () => {
  // 首屏并行准备字典、机构选项和第一页数据。
  await Promise.all([loadDictOptions(), fetchSanatoriumOptions(), fetchElders()]);
});
</script>

<style scoped>
.elder-page {
  --bg: #f3f6fa;
  --panel: #ffffff;
  --line: #d7e0ea;
  --text: #1a2f4d;
  --muted: #5d728f;
  --accent: #3b6fb1;
  --shadow: 0 2px 8px rgba(26, 47, 77, 0.06);

  min-height: calc(100vh - 56px);
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
  box-sizing: border-box;
  padding: 14px;
  background: var(--bg);
}

* {
  box-sizing: border-box;
  min-width: 0;
}

.card-shell {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: var(--shadow);
}

.page-header {
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.page-header h1 {
  margin: 0;
  color: var(--text);
  font-size: 38px;
  line-height: 1.2;
}

.page-header p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.head-stats {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.chip {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #d6dee7;
  color: #5d728f;
  background: #f8fafc;
  font-size: 13px;
}

.chip.active {
  color: #3f7f22;
  border-color: #b9d9aa;
  background: #f3fbef;
}

.filter-panel {
  margin-top: 12px;
  padding: 12px 14px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.toolbar {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e6edf5;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.table-panel {
  margin-top: 12px;
  padding: 12px;
}

.cell-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cell-stack strong {
  color: var(--text);
  font-size: 14px;
  line-height: 1.35;
}

.cell-stack span {
  color: #4f6687;
  font-size: 12px;
  line-height: 1.35;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-form-item) {
  margin-bottom: 0;
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

:deep(.el-table__header th) {
  background: #f8fafc;
  color: #5d728f;
  font-weight: 700;
}

:deep(.el-table td.el-table__cell),
:deep(.el-table th.el-table__cell) {
  padding: 11px 0;
}

:deep(.el-pagination) {
  flex-wrap: wrap;
  row-gap: 8px;
}

:deep(.elder-form-dialog .el-dialog) {
  border: 1px solid #d7e0ea;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 8px 20px rgba(26, 47, 77, 0.1);
}

:deep(.elder-form-dialog .el-dialog__header) {
  margin: 0;
  padding: 18px 22px 14px;
  border-bottom: 1px solid #e6edf5;
  background: #ffffff;
}

:deep(.elder-form-dialog .el-dialog__body) {
  padding: 18px 22px 12px;
  background: #ffffff;
}

:deep(.elder-form-dialog .el-dialog__footer) {
  padding: 12px 22px 16px;
  border-top: 1px solid #e6edf5;
  background: #ffffff;
}

:deep(.elder-form-dialog .el-input__wrapper),
:deep(.elder-form-dialog .el-select__wrapper),
:deep(.elder-form-dialog .el-textarea__inner) {
  border-color: #dbe5f1;
  background-color: #ffffff;
}

:deep(.elder-form-dialog .el-input__wrapper.is-focus),
:deep(.elder-form-dialog .el-select__wrapper.is-focused),
:deep(.elder-form-dialog .el-textarea__inner:focus) {
  border-color: #7d9bc2;
  box-shadow: 0 0 0 2px rgba(125, 155, 194, 0.14);
}

:deep(.elder-form-dialog .el-input-number) {
  border: 1px solid #dbe5f1;
  border-radius: 8px;
  overflow: hidden;
}

:deep(.elder-form-dialog .el-input-number .el-input__wrapper) {
  border: none;
  box-shadow: none;
}

:deep(.elder-form-dialog .el-input-number__decrease),
:deep(.elder-form-dialog .el-input-number__increase) {
  border-color: #e3eaf3;
  background: #f8fbff;
  color: #5d728f;
}

:deep(.elder-attachment-dialog .el-dialog) {
  border: 1px solid #d7e0ea;
  border-radius: 10px;
  overflow: hidden;
}

:deep(.elder-attachment-dialog .el-dialog__header) {
  margin: 0;
  padding: 18px 22px 14px;
  border-bottom: 1px solid #e6edf5;
}

:deep(.elder-attachment-dialog .el-dialog__body) {
  padding: 18px 22px 20px;
}

.attachment-upload-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding-bottom: 14px;
  border-bottom: 1px solid #e6edf5;
}

.attachment-section {
  margin-top: 16px;
}

.section-title {
  margin-bottom: 10px;
  color: var(--text);
  font-size: 15px;
  font-weight: 700;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.image-card {
  border: 1px solid #dbe5f1;
  border-radius: 8px;
  padding: 8px;
  background: #f8fafc;
}

.elder-image {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 6px;
  background: #eef3f8;
}

.attachment-name {
  margin-top: 8px;
  color: #344966;
  font-size: 12px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1480px) {
  .filter-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .elder-page {
    padding: 12px;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .pager {
    justify-content: center;
  }

  .image-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
