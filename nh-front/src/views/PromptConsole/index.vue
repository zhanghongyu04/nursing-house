<template>
  <main class="prompt-console-page">
    <div class="console-shell">
      <section class="hero-panel">
        <div class="hero-copy">
          <span class="eyebrow">Prompt Ops Console</span>
          <h1>内置提示词控制台</h1>
          <p>面向驻场工程师和开发权限账号。新增、编辑和启停会写入审计日志；同步 Redis 不保证已构建的 ChatClient 立即热生效。</p>
        </div>
        <div class="hero-actions">
          <el-button v-if="canManagePrompt" size="large" @click="openCreateDialog()">新增版本提示词</el-button>
          <el-button v-if="canViewTemplates" size="large" type="primary" :loading="loading" @click="loadTemplates">刷新</el-button>
        </div>
      </section>

      <section v-if="canViewTemplates" class="metric-grid">
        <div class="metric-card">
          <span>提示词类型</span>
          <strong>{{ promptNames.length }}</strong>
        </div>
        <div class="metric-card">
          <span>版本总数</span>
          <strong>{{ templates.length }}</strong>
        </div>
        <div class="metric-card">
          <span>启用版本</span>
          <strong>{{ activeVersionCount }}</strong>
        </div>
        <div class="metric-card">
          <span>当前查看</span>
          <strong>{{ currentVersionLabel }}</strong>
        </div>
      </section>

      <section v-if="canViewTemplates" class="workspace-grid">
        <aside class="version-panel">
          <div class="panel-header">
            <div>
              <h2>版本列表</h2>
              <p>选择提示词并管理版本状态</p>
            </div>
          </div>

          <div class="prompt-filter">
            <el-select v-model="selectedPromptName" placeholder="选择提示词" filterable @change="loadPromptDetail">
              <el-option v-for="item in promptNames" :key="item" :label="item" :value="item" />
            </el-select>
            <el-button v-if="canViewPromptDetail" :disabled="!selectedPromptName" @click="loadPromptDetail">当前启用</el-button>
          </div>

          <el-table
            v-loading="loading"
            :data="templates"
            class="version-table"
            max-height="620"
            highlight-current-row
            @row-click="row => loadPromptVersion(row.promptName, row.version)"
          >
            <el-table-column label="版本" width="92">
              <template #default="{ row }">
                <div class="version-cell">
                  <strong>v{{ row.version }}</strong>
                  <span>{{ row.segmentCount }} 段</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="promptName" label="名称" min-width="150" show-overflow-tooltip />
            <el-table-column label="状态" width="82">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'info'" effect="light">
                  {{ row.status === 0 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="canManagePrompt" label="操作" width="128" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button link type="primary" @click.stop="loadPromptVersion(row.promptName, row.version)">查看</el-button>
                  <el-button v-if="row.status !== 0" link type="success" @click.stop="handleActivate(row.promptName, row.version)">启用</el-button>
                  <el-button v-else link type="warning" @click.stop="handleDisableVersion(row.promptName, row.version)">停用</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </aside>

        <section class="detail-panel">
          <div v-if="activePrompt" class="detail-header">
            <div>
              <span class="eyebrow">Current Version</span>
              <h2>{{ activePrompt.promptName }} v{{ activePrompt.version }}</h2>
              <p>{{ activePrompt.segments.length }} 个片段，Redis Key：{{ cacheView?.redisKey || '--' }}</p>
            </div>
            <div class="detail-actions">
              <el-button v-if="canSyncPrompt" :disabled="!selectedPromptName" :loading="syncing" @click="handleSync">同步 Redis</el-button>
              <el-button @click="showActiveOverview">查看生效总览</el-button>
              <el-button v-if="canManagePrompt" type="primary" @click="openCreateSegmentDialog">新增片段</el-button>
            </div>
          </div>

          <div v-else class="detail-header">
            <div>
              <span class="eyebrow">Active Prompts</span>
              <h2>当前生效提示词</h2>
              <p>展示系统当前每类提示词的最高启用版本，点击“查看”进入单版本详情。</p>
            </div>
            <div class="detail-actions">
              <el-button :loading="loadingActiveOverview" @click="loadActivePromptOverview">刷新总览</el-button>
            </div>
          </div>

          <div v-if="!activePrompt" v-loading="loadingActiveOverview" class="active-overview">
            <el-table :data="activePromptOverview" max-height="620" class="overview-table">
              <el-table-column prop="promptName" label="提示词名称" min-width="180" show-overflow-tooltip />
              <el-table-column label="生效版本" width="110">
                <template #default="{ row }">v{{ row.version }}</template>
              </el-table-column>
              <el-table-column label="片段数" width="90">
                <template #default="{ row }">{{ row.segments.length }}</template>
              </el-table-column>
              <el-table-column label="合并内容预览" min-width="360">
                <template #default="{ row }">
                  <div class="content-preview">{{ row.mergedContent }}</div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="canViewPromptDetail" link type="primary" @click="loadPromptVersion(row.promptName, row.version)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!loadingActiveOverview && activePromptOverview.length === 0" description="暂无生效提示词" />
          </div>

          <el-tabs v-if="activePrompt" v-model="activeTab" class="detail-tabs">
            <el-tab-pane label="片段编辑" name="segments">
              <el-table :data="activePrompt.segments" max-height="540" class="segment-table">
                <el-table-column prop="promptIndex" label="#" width="70" />
                <el-table-column label="内容" min-width="460">
                  <template #default="{ row }">
                    <div class="content-preview">{{ row.promptContent }}</div>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 0 ? 'success' : 'info'" effect="light">
                      {{ row.status === 0 ? '启用' : '停用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column v-if="canManagePrompt" label="操作" width="150" fixed="right">
                  <template #default="{ row }">
                    <div class="table-actions">
                      <el-button link type="primary" @click="openEditSegment(row)">编辑</el-button>
                      <el-button v-if="row.status !== 0" link type="success" @click="handleEnableSegment(row)">启用</el-button>
                      <el-button v-else link type="warning" @click="handleDisableSegment(row)">停用</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane v-if="canPreviewPrompt" label="合并预览" name="preview">
              <el-input class="code-textarea" :model-value="activePrompt.mergedContent" type="textarea" :rows="22" readonly />
            </el-tab-pane>

            <el-tab-pane v-if="canSyncPrompt" label="Redis 缓存" name="cache">
              <div class="cache-bar">
                <span>{{ cacheView?.redisKey || '--' }}</span>
                <el-button size="small" :loading="syncing" @click="handleSync">重新同步</el-button>
              </div>
              <el-input class="code-textarea" :model-value="cacheView?.content || ''" type="textarea" :rows="20" readonly />
            </el-tab-pane>

            <el-tab-pane v-if="canViewLogs" label="审计日志" name="logs">
              <el-table :data="logs" max-height="520" class="log-table">
                <el-table-column prop="operationType" label="操作" width="160" />
                <el-table-column prop="operator" label="操作人" width="110" />
                <el-table-column prop="newVersion" label="版本" width="80" />
                <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
                <el-table-column prop="createTime" label="时间" min-width="170" />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </section>
      </section>

      <section v-else class="empty-state-panel">
        <el-empty description="当前账号无提示词模板查看权限" />
      </section>
    </div>

    <el-dialog v-if="canManagePrompt" v-model="createDialogVisible" title="新增版本提示词" width="780px" destroy-on-close>
      <el-form label-width="110px">
        <el-form-item label="提示词名称">
          <el-select
            v-model="createForm.promptName"
            placeholder="请选择提示词类型"
            style="width: 100%"
            @change="handleCreatePromptNameChange"
          >
            <el-option
              v-for="item in supportedPromptTypes"
              :key="item.promptName"
              :label="`${item.label} (${item.promptName})`"
              :value="item.promptName"
            >
              <div class="type-option">
                <strong>{{ item.label }}</strong>
                <span>{{ item.promptName }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedCreateType" label="说明">
          <p class="type-description">{{ selectedCreateType.description }}</p>
        </el-form-item>
        <el-form-item label="目标版本">
          <el-tag type="primary" effect="light">{{ createTargetVersionLabel }}</el-tag>
        </el-form-item>
        <el-form-item label="片段内容">
          <div class="segment-editor-list">
            <div v-for="(segment, index) in createForm.segments" :key="index" class="segment-editor-item">
              <el-input-number v-model="segment.promptIndex" :min="1" />
              <el-input v-model="segment.promptContent" type="textarea" :rows="4" placeholder="请输入提示词片段" />
              <el-button link type="danger" @click="removeCreateSegment(index)">删除</el-button>
            </div>
          </div>
          <el-button @click="addCreateSegment">添加片段</el-button>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" maxlength="200" />
        </el-form-item>
        <el-form-item label="新增后启用">
          <el-switch v-model="createForm.syncToRedis" active-text="启用并同步 Redis" inactive-text="仅创建停用版本" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreateVersion">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-if="canManagePrompt" v-model="createSegmentDialogVisible" title="为当前版本新增片段" width="760px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="当前版本">
          <el-input :model-value="`${createSegmentForm.promptName} v${createSegmentForm.version}`" disabled />
        </el-form-item>
        <el-form-item label="片段序号">
          <el-input-number v-model="createSegmentForm.promptIndex" :min="1" />
          <span class="form-tip">默认追加到当前版本末尾，可手动调整排序。</span>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="createSegmentForm.promptContent" type="textarea" :rows="12" placeholder="请输入新增片段内容" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createSegmentForm.remark" maxlength="200" />
        </el-form-item>
        <el-form-item label="同步">
          <el-switch v-model="createSegmentForm.syncToRedis" active-text="新增后同步 Redis" inactive-text="仅保存数据库" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createSegmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreateSegment">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-if="canManagePrompt" v-model="editDialogVisible" title="编辑提示词片段" width="760px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="版本">
          <el-input :model-value="`${editForm.promptName} v${editForm.version}`" disabled />
        </el-form-item>
        <el-form-item label="片段">
          <el-input :model-value="`#${editForm.promptIndex} / ID ${editForm.id}`" disabled />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="editForm.promptContent" type="textarea" :rows="14" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" maxlength="200" />
        </el-form-item>
        <el-form-item label="同步">
          <el-switch v-model="editForm.syncToRedis" active-text="保存后同步 Redis" inactive-text="仅保存数据库" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleUpdateSegment">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useUserStore } from "@/stores/userStore";
import { AUTH_ROLES, hasAnyRole, hasResourcePath } from "@/constants/authRoles";
import {
  activatePromptVersionAPI,
  createPromptSegmentAPI,
  createPromptVersionAPI,
  disablePromptSegmentAPI,
  disablePromptVersionAPI,
  enablePromptSegmentAPI,
  getActivePromptAPI,
  getPromptCacheAPI,
  getPromptVersionAPI,
  listSupportedPromptTypesAPI,
  listPromptLogsAPI,
  listPromptTemplatesAPI,
  syncPromptAPI,
  updatePromptSegmentAPI,
  type PromptTemplateActiveView,
  type PromptTemplateCacheView,
  type PromptTemplateDetail,
  type PromptTemplateLogView,
  type PromptTemplateSummary,
  type PromptSupportedTypeView
} from "@/api/promptConsole";

interface SegmentEditForm {
  promptIndex: number;
  promptContent: string;
}

const templates = ref<PromptTemplateSummary[]>([]);
const supportedPromptTypes = ref<PromptSupportedTypeView[]>([]);
const selectedPromptName = ref("");
const activePrompt = ref<PromptTemplateActiveView | null>(null);
const activePromptOverview = ref<PromptTemplateActiveView[]>([]);
const cacheView = ref<PromptTemplateCacheView | null>(null);
const logs = ref<PromptTemplateLogView[]>([]);
const loading = ref(false);
const loadingActiveOverview = ref(false);
const syncing = ref(false);
const saving = ref(false);
const activeTab = ref("segments");
const createDialogVisible = ref(false);
const createSegmentDialogVisible = ref(false);
const editDialogVisible = ref(false);
const userStore = useUserStore();

const createForm = reactive({
  promptName: "",
  segments: [] as SegmentEditForm[],
  remark: "",
  syncToRedis: false
});

const createSegmentForm = reactive({
  promptName: "",
  version: 0,
  promptIndex: 1,
  promptContent: "",
  remark: "",
  syncToRedis: false
});

const editForm = reactive({
  id: 0,
  promptName: "",
  version: 0,
  promptIndex: 0,
  promptContent: "",
  remark: "",
  syncToRedis: false
});

const promptNames = computed(() => Array.from(new Set(templates.value.map(item => item.promptName))));
const canAccessPromptConsole = computed(() => {
  return hasAnyRole(userStore.userInfo.roleLabels, [AUTH_ROLES.GOV_ADMIN])
    && hasResourcePath(userStore.userInfo.resourcePaths, "/web/agent/prompt/page");
});
const canViewTemplates = computed(() => canAccessPromptConsole.value && hasResourcePath(userStore.userInfo.resourcePaths, ["/web/agent/prompt/templates", "/web/agent/prompt/templates/**"]));
const canViewPromptDetail = computed(() => canAccessPromptConsole.value && hasResourcePath(userStore.userInfo.resourcePaths, "/web/agent/prompt/templates/**"));
const canManagePrompt = computed(() => canAccessPromptConsole.value && hasResourcePath(userStore.userInfo.resourcePaths, "/web/agent/prompt/templates/**"));
const canPreviewPrompt = computed(() => canAccessPromptConsole.value && hasResourcePath(userStore.userInfo.resourcePaths, "/web/agent/prompt/preview"));
const canSyncPrompt = computed(() => canAccessPromptConsole.value && hasResourcePath(userStore.userInfo.resourcePaths, "/web/agent/prompt/sync"));
const canViewLogs = computed(() => canAccessPromptConsole.value && hasResourcePath(userStore.userInfo.resourcePaths, "/web/agent/prompt/logs"));
const selectedCreateType = computed(() => supportedPromptTypes.value.find(item => item.promptName === createForm.promptName));
const createTargetVersionLabel = computed(() => {
  if (!selectedCreateType.value) return "--";
  const maxVersion = getPromptMaxVersion(createForm.promptName);
  return `v${maxVersion + 1}`;
});
const activeVersionCount = computed(() => templates.value.filter(item => item.status === 0).length);
const currentVersionLabel = computed(() => activePrompt.value ? `v${activePrompt.value.version}` : "--");

const loadTemplates = async () => {
  if (!canViewTemplates.value) {
    templates.value = [];
    activePromptOverview.value = [];
    activePrompt.value = null;
    cacheView.value = null;
    logs.value = [];
    return;
  }
  loading.value = true;
  try {
    const [templateRes, supportedRes] = await Promise.all([
      listPromptTemplatesAPI(),
      listSupportedPromptTypesAPI()
    ]);
    supportedPromptTypes.value = supportedRes.data || [];
    const res = templateRes;
    templates.value = res.data || [];
    if (!selectedPromptName.value && promptNames.value.length > 0) {
      selectedPromptName.value = promptNames.value[0];
    }
    await loadActivePromptOverview();
  } finally {
    loading.value = false;
  }
};

const loadActivePromptOverview = async () => {
  if (!canViewPromptDetail.value) {
    activePromptOverview.value = [];
    activePrompt.value = null;
    cacheView.value = null;
    logs.value = [];
    return;
  }
  loadingActiveOverview.value = true;
  try {
    activePrompt.value = null;
    cacheView.value = null;
    logs.value = [];
    const results = await Promise.allSettled(
      promptNames.value.map(promptName => getActivePromptAPI(promptName))
    );
    activePromptOverview.value = results
      .filter((result): result is PromiseFulfilledResult<{ data: PromptTemplateActiveView }> => result.status === "fulfilled")
      .map(result => result.value.data)
      .filter(Boolean);
  } finally {
    loadingActiveOverview.value = false;
  }
};

const showActiveOverview = async () => {
  activeTab.value = "segments";
  await loadActivePromptOverview();
};

const loadPromptDetail = async () => {
  if (!selectedPromptName.value || !canViewPromptDetail.value) return;
  const activeRes = await getActivePromptAPI(selectedPromptName.value);
  const [cacheRes, logsRes] = await Promise.all([
    canSyncPrompt.value ? getPromptCacheAPI(selectedPromptName.value) : Promise.resolve(null),
    canViewLogs.value ? listPromptLogsAPI({ promptName: selectedPromptName.value, limit: 20 }) : Promise.resolve(null)
  ]);
  activePrompt.value = activeRes.data;
  cacheView.value = cacheRes?.data || null;
  logs.value = logsRes?.data || [];
};

const loadPromptVersion = async (promptName: string, version: number) => {
  if (!canViewPromptDetail.value) return;
  selectedPromptName.value = promptName;
  const versionRes = await getPromptVersionAPI(promptName, version);
  const [cacheRes, logsRes] = await Promise.all([
    canSyncPrompt.value ? getPromptCacheAPI(promptName) : Promise.resolve(null),
    canViewLogs.value ? listPromptLogsAPI({ promptName, limit: 20 }) : Promise.resolve(null)
  ]);
  activePrompt.value = versionRes.data;
  cacheView.value = cacheRes?.data || null;
  logs.value = logsRes?.data || [];
};

const refreshAfterWrite = async (promptName = selectedPromptName.value, version?: number) => {
  await loadTemplates();
  if (promptName && version) {
    await loadPromptVersion(promptName, version);
  } else if (promptName) {
    selectedPromptName.value = promptName;
    await loadPromptDetail();
  }
};

const handleSync = async () => {
  if (!selectedPromptName.value || !canSyncPrompt.value) return;
  await ElMessageBox.confirm(
    "同步会把数据库中最新启用版本写入 Redis。当前阶段不保证已构建 ChatClient 立即热生效，确认继续吗？",
    "同步确认",
    { type: "warning", confirmButtonText: "同步", cancelButtonText: "取消" }
  );
  syncing.value = true;
  try {
    await syncPromptAPI(selectedPromptName.value);
    ElMessage.success("已同步到 Redis");
    await loadPromptDetail();
  } finally {
    syncing.value = false;
  }
};

const openCreateDialog = (promptName?: string) => {
  if (!canManagePrompt.value) return;
  const supportedNames = new Set(supportedPromptTypes.value.map(item => item.promptName));
  const explicitPromptName = typeof promptName === "string" && supportedNames.has(promptName) ? promptName : "";
  const currentPromptName = activePrompt.value?.promptName && supportedNames.has(activePrompt.value.promptName)
    ? activePrompt.value.promptName
    : "";
  const selectedSupportedPromptName = selectedPromptName.value && supportedNames.has(selectedPromptName.value)
    ? selectedPromptName.value
    : "";
  const targetPromptName = explicitPromptName || currentPromptName || selectedSupportedPromptName || supportedPromptTypes.value[0]?.promptName || "";
  createForm.promptName = targetPromptName;
  createForm.segments = [{ promptIndex: 1, promptContent: "" }];
  createForm.remark = "";
  createForm.syncToRedis = false;
  createDialogVisible.value = true;
};

const handleCreatePromptNameChange = () => {
  createForm.segments = [{ promptIndex: 1, promptContent: "" }];
};

const getMaxVersion = (promptName: string) => {
  return templates.value
    .filter(item => item.promptName === promptName)
    .reduce((max, item) => Math.max(max, item.version), 0);
};

const getPromptMaxVersion = (promptName: string) => {
  const supportedMaxVersion = supportedPromptTypes.value.find(item => item.promptName === promptName)?.maxVersion ?? 0;
  return Math.max(supportedMaxVersion, getMaxVersion(promptName));
};

const addCreateSegment = () => {
  createForm.segments.push({ promptIndex: createForm.segments.length + 1, promptContent: "" });
};

const removeCreateSegment = (index: number) => {
  createForm.segments.splice(index, 1);
};

const openCreateSegmentDialog = () => {
  if (!activePrompt.value || !canManagePrompt.value) return;
  createSegmentForm.promptName = activePrompt.value.promptName;
  createSegmentForm.version = activePrompt.value.version;
  createSegmentForm.promptIndex = activePrompt.value.segments
    .map(item => item.promptIndex)
    .filter(Boolean)
    .reduce((max, item) => Math.max(max, item), 0) + 1;
  createSegmentForm.promptContent = "";
  createSegmentForm.remark = "";
  createSegmentForm.syncToRedis = false;
  createSegmentDialogVisible.value = true;
};

const handleCreateSegment = async () => {
  if (!canManagePrompt.value) return;
  if (!createSegmentForm.promptName || !createSegmentForm.version) {
    ElMessage.warning("请先选择提示词版本");
    return;
  }
  if (!createSegmentForm.promptContent.trim()) {
    ElMessage.warning("提示词片段内容不能为空");
    return;
  }
  saving.value = true;
  try {
    const res = await createPromptSegmentAPI(createSegmentForm.promptName, createSegmentForm.version, {
      promptIndex: createSegmentForm.promptIndex,
      promptContent: createSegmentForm.promptContent.trim(),
      remark: createSegmentForm.remark,
      syncToRedis: createSegmentForm.syncToRedis
    });
    createSegmentDialogVisible.value = false;
    ElMessage.success("片段已新增");
    await refreshAfterWrite(res.data.promptName, res.data.version);
  } finally {
    saving.value = false;
  }
};

const handleCreateVersion = async () => {
  if (!canManagePrompt.value) return;
  if (!createForm.promptName) {
    ElMessage.warning("请选择提示词类型");
    return;
  }
  const segments = createForm.segments
    .filter(item => item.promptContent.trim())
    .map(item => ({ promptIndex: item.promptIndex, promptContent: item.promptContent.trim() }));
  if (segments.length === 0) {
    ElMessage.warning("请至少填写一个提示词片段");
    return;
  }
  saving.value = true;
  try {
    const res = await createPromptVersionAPI(createForm.promptName, {
      segments,
      remark: createForm.remark,
      syncToRedis: createForm.syncToRedis
    });
    createDialogVisible.value = false;
    selectedPromptName.value = res.data.promptName;
    ElMessage.success("提示词版本已创建");
    await refreshAfterWrite(res.data.promptName, res.data.version);
  } finally {
    saving.value = false;
  }
};

const openEditSegment = (row: PromptTemplateDetail) => {
  if (!canManagePrompt.value) return;
  editForm.id = row.id;
  editForm.promptName = row.promptName;
  editForm.version = row.version;
  editForm.promptIndex = row.promptIndex;
  editForm.promptContent = row.promptContent;
  editForm.remark = "";
  editForm.syncToRedis = false;
  editDialogVisible.value = true;
};

const handleUpdateSegment = async () => {
  if (!canManagePrompt.value) return;
  if (!editForm.id || !editForm.promptContent.trim()) {
    ElMessage.warning("提示词片段内容不能为空");
    return;
  }
  saving.value = true;
  try {
    const res = await updatePromptSegmentAPI(editForm.id, {
      promptContent: editForm.promptContent.trim(),
      remark: editForm.remark,
      syncToRedis: editForm.syncToRedis
    });
    editDialogVisible.value = false;
    ElMessage.success("片段已更新");
    await refreshAfterWrite(res.data.promptName, res.data.version);
  } finally {
    saving.value = false;
  }
};

const handleActivate = async (promptName: string, version: number) => {
  if (!canManagePrompt.value) return;
  await ElMessageBox.confirm(`确认启用 ${promptName} v${version}？启用后会同步 Redis。`, "启用提示词版本", {
    type: "warning",
    confirmButtonText: "启用",
    cancelButtonText: "取消"
  });
  const res = await activatePromptVersionAPI(promptName, version, { syncToRedis: true });
  ElMessage.success("版本已启用");
  await refreshAfterWrite(res.data.promptName, res.data.version);
};

const handleDisableVersion = async (promptName: string, version: number) => {
  if (!canManagePrompt.value) return;
  await ElMessageBox.confirm(`确认停用 ${promptName} v${version}？如果这是唯一启用版本，后续同步可能失败。`, "停用提示词版本", {
    type: "warning",
    confirmButtonText: "停用",
    cancelButtonText: "取消"
  });
  const res = await disablePromptVersionAPI(promptName, version, { syncToRedis: false });
  ElMessage.success("版本已停用");
  await refreshAfterWrite(res.data.promptName, res.data.version);
};

const handleEnableSegment = async (row: PromptTemplateDetail) => {
  if (!canManagePrompt.value) return;
  const res = await enablePromptSegmentAPI(row.id, { syncToRedis: false });
  ElMessage.success("片段已启用");
  await refreshAfterWrite(res.data.promptName, res.data.version);
};

const handleDisableSegment = async (row: PromptTemplateDetail) => {
  if (!canManagePrompt.value) return;
  await ElMessageBox.confirm("确认停用该提示词片段？", "停用片段", {
    type: "warning",
    confirmButtonText: "停用",
    cancelButtonText: "取消"
  });
  const res = await disablePromptSegmentAPI(row.id, { syncToRedis: false });
  ElMessage.success("片段已停用");
  await refreshAfterWrite(res.data.promptName, res.data.version);
};

onMounted(async () => {
  await userStore.syncCurrentUserInfo();
  await loadTemplates();
});
</script>

<style scoped>
.prompt-console-page {
  min-height: calc(100vh - 60px);
  padding: 24px 24px 28px;
  background: #f3f6fa;
  color: #1a2f4d;
}

.console-shell {
  max-width: 1440px;
  margin: 0 auto;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: center;
  padding: 12px 20px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  border: 1px solid #d7e0ea;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(26, 47, 77, 0.05);
}

.hero-copy h1 {
  margin: 6px 0 8px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0;
}

.hero-copy p {
  max-width: 840px;
  margin: 0;
  color: #5d728f;
  line-height: 1.7;
}

.eyebrow {
  color: #3b6fb1;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-actions,
.detail-actions,
.prompt-filter,
.table-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin: 12px 0;
}

.metric-card {
  padding: 16px 18px;
  background: #ffffff;
  border: 1px solid #d7e0ea;
  border-radius: 10px;
}

.metric-card span {
  display: block;
  margin-bottom: 8px;
  color: #5d728f;
  font-size: 13px;
}

.metric-card strong {
  font-size: 24px;
  font-weight: 700;
  color: #1a2f4d;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(420px, 0.92fr) minmax(0, 1.5fr);
  gap: 18px;
  align-items: start;
}

.version-panel,
.detail-panel {
  min-width: 0;
  background: #ffffff;
  border: 1px solid #d7e0ea;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(26, 47, 77, 0.05);
}

.version-panel {
  padding: 18px;
}

.detail-panel {
  padding: 20px;
}

.panel-header,
.detail-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.panel-header h2,
.detail-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.panel-header p,
.detail-header p {
  margin: 6px 0 0;
  color: #5d728f;
  line-height: 1.5;
}

.prompt-filter {
  margin: 18px 0 14px;
}

.prompt-filter .el-select {
  flex: 1;
}

.version-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.version-cell strong {
  font-weight: 700;
  color: #1a2f4d;
}

.version-cell span {
  color: #5d728f;
  font-size: 12px;
}

.detail-tabs {
  margin-top: 18px;
}

.active-overview {
  margin-top: 18px;
}

.content-preview {
  display: -webkit-box;
  max-height: 56px;
  overflow: hidden;
  color: #334155;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  word-break: break-word;
}

.cache-bar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #f8fafc;
  border: 1px solid #d9e1ea;
  border-radius: 8px;
}

.cache-bar span {
  min-width: 0;
  overflow: hidden;
  color: #5d728f;
  font-family: Consolas, monospace;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-textarea :deep(.el-textarea__inner) {
  color: #1e293b;
  font-family: Consolas, "Courier New", monospace;
  line-height: 1.7;
  background: #f8fafc;
  border-color: #d7e0ea;
}

.form-tip {
  margin-left: 12px;
  color: #5d728f;
  font-size: 13px;
}

:deep(.el-form-item__content) {
  min-width: 0;
}

.segment-editor-list {
  width: 100%;
  min-width: 0;
}

.segment-editor-item {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: start;
  width: 100%;
  min-width: 0;
  margin-bottom: 10px;
}

.segment-editor-item :deep(.el-input-number) {
  width: 120px;
}

.segment-editor-item :deep(.el-textarea) {
  min-width: 0;
}

.segment-editor-item .el-button {
  justify-self: end;
  padding-left: 0;
  padding-right: 0;
}

.type-option {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.type-option strong {
  color: #1a2f4d;
  font-weight: 600;
}

.type-option span,
.type-description {
  color: #5d728f;
}

.type-description {
  margin: 0;
  line-height: 1.6;
}

:deep(.el-button) {
  min-height: 36px;
}

:deep(.el-button--large) {
  min-height: 42px;
}

:deep(.el-table) {
  color: #1a2f4d;
  --el-table-header-bg-color: #f8fafc;
  --el-table-border-color: #d9e1ea;
  --el-table-row-hover-bg-color: #edf3fb;
}

:deep(.el-table th.el-table__cell) {
  color: #5d728f;
  font-weight: 700;
}

:deep(.el-tabs__item) {
  font-weight: 600;
}

:deep(.el-button--primary:not(.is-link)) {
  background: #3b6fb1;
  border-color: #3b6fb1;
}

:deep(.el-button--primary:not(.is-link):hover),
:deep(.el-button--primary:not(.is-link):focus) {
  background: #2f5e9e;
  border-color: #2f5e9e;
}

:deep(.el-button--primary:not(.is-link):active) {
  background: #254f87;
  border-color: #254f87;
}

:deep(.el-button.is-link.el-button--primary) {
  background: transparent;
  border-color: transparent;
  color: #2f5e9e;
}

:deep(.el-button.is-link.el-button--primary:hover),
:deep(.el-button.is-link.el-button--primary:focus) {
  background: transparent;
  border-color: transparent;
  color: #254f87;
}

@media (max-width: 1180px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .prompt-console-page {
    padding: 24px 14px 22px;
  }

  .hero-panel,
  .detail-header {
    grid-template-columns: 1fr;
  }

  .hero-panel,
  .panel-header,
  .detail-header {
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-actions,
  .detail-actions,
  .prompt-filter {
    width: 100%;
    flex-wrap: wrap;
  }

  .prompt-filter .el-select,
  .hero-actions .el-button,
  .detail-actions .el-button {
    width: 100%;
  }

  .segment-editor-item {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 520px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
