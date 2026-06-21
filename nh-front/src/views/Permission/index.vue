<template>
  <div class="permission-page">
    <el-alert
      v-if="!canAccess"
      title="当前账号无权访问角色授权"
      type="error"
      show-icon
      :closable="false"
      description="请在权限树中授予角色授权相关资源后再访问。"
      class="mb-16"
    />

    <template v-else>
      <div class="page-header">
        <div>
          <h2 class="page-title">角色授权</h2>
          <p class="page-desc">按角色维护目录、菜单、按钮与接口资源授权，保存后对对应角色生效。</p>
        </div>
        <el-button v-if="canSave" class="save-btn" type="primary" :icon="Check" :loading="saveLoading" :disabled="!selectedRoleId" @click="handleSave">
          保存授权
        </el-button>
      </div>

      <div class="permission-layout">
        <el-card shadow="never" class="role-card">
          <template #header>
            <div class="card-header">
              <span>角色列表</span>
              <el-button text :icon="Refresh" :loading="roleLoading" @click="fetchRoles">刷新</el-button>
            </div>
          </template>

          <el-scrollbar height="calc(100vh - 260px)">
            <div v-loading="roleLoading" class="role-list">
              <button
                v-for="role in roleList"
                :key="getRoleId(role)"
                type="button"
                class="role-item"
                :class="{ active: selectedRoleId === getRoleId(role) }"
                @click="selectRole(role)"
              >
                <span class="role-name">{{ getRoleName(role) }}</span>
                <span class="role-code">{{ getRoleCode(role) }}</span>
              </button>
              <el-empty v-if="!roleLoading && roleList.length === 0" description="暂无角色数据" />
            </div>
          </el-scrollbar>
        </el-card>

        <el-card shadow="never" class="resource-card">
          <template #header>
            <div class="card-header">
              <div>
                <span>资源授权</span>
                <small v-if="selectedRoleName">当前角色：{{ selectedRoleName }}</small>
              </div>
              <div class="tree-actions">
                <el-button :icon="CircleCheck" @click="checkAllResources" :disabled="resourceTree.length === 0">全选</el-button>
                <el-button :icon="Remove" @click="clearCheckedResources" :disabled="resourceTree.length === 0">清空</el-button>
                <el-button :icon="Refresh" @click="reloadCurrentRole" :loading="resourceLoading" :disabled="!selectedRoleId">重载</el-button>
              </div>
            </div>
          </template>

          <div class="tree-panel" v-loading="resourceLoading">
            <el-tree
              ref="resourceTreeRef"
              class="resource-tree"
              :data="resourceTree"
              node-key="resourceNo"
              show-checkbox
              :props="treeProps"
              empty-text="暂无资源数据"
            >
              <template #default="{ data }">
                <div class="tree-node">
                  <span class="node-title" :title="getResourceName(data)">{{ getResourceName(data) }}</span>
                  <el-tag class="resource-type-tag" size="small" :type="getResourceTagType(data)">
                    {{ getResourceTypeLabel(data) }}
                  </el-tag>
                  <span v-if="getResourcePath(data)" class="node-path" :title="getResourcePath(data)">{{ getResourcePath(data) }}</span>
                  <span v-else class="node-path muted">-</span>
                </div>
              </template>
            </el-tree>
          </div>
        </el-card>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Check, CircleCheck, Refresh, Remove } from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/userStore';
import { hasResourcePath } from '@/constants/authRoles.ts';
import {
  getPermissionResourceTreeAPI,
  getPermissionRolesAPI,
  getRoleResourceNosAPI,
  updateRoleResourceNosAPI,
  type PermissionResourceNode,
  type PermissionRole,
} from '@/api/permission.ts';

interface PermissionTreeRef {
  setCheckedKeys: (keys: Array<string | number>, leafOnly?: boolean) => void;
  getCheckedKeys: (leafOnly?: boolean) => Array<string | number>;
  getHalfCheckedKeys: () => Array<string | number>;
}

const userStore = useUserStore();
const canAccess = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/permission/roles'));
const canSave = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/permission/roles/*/resources'));

const roleList = ref<PermissionRole[]>([]);
const resourceTree = ref<PermissionResourceNode[]>([]);
const selectedRoleId = ref<number | string>('');
const selectedRoleName = ref('');
const roleLoading = ref(false);
const resourceLoading = ref(false);
const saveLoading = ref(false);
const resourceTreeRef = ref<PermissionTreeRef>();

const treeProps = {
  label: (data: PermissionResourceNode) => getResourceName(data),
  children: 'children',
};

const typeLabelMap: Record<string, string> = {
  S: '平台',
  C: '目录',
  M: '菜单',
  R: '按钮/接口',
  DIRECTORY: '目录',
  DIR: '目录',
  CATALOG: '目录',
  MENU: '菜单',
  BUTTON: '按钮',
  BTN: '按钮',
  API: '接口',
  INTERFACE: '接口',
  0: '目录',
  1: '菜单',
  2: '按钮',
  3: '接口',
};

const getRoleId = (role: PermissionRole) => role.id ?? role.roleId ?? '';
const getRoleName = (role: PermissionRole) => role.roleName || role.name || getRoleCode(role) || `角色#${getRoleId(role)}`;
const getRoleCode = (role: PermissionRole) => role.roleCode || role.code || '';

const getResourceName = (node: PermissionResourceNode) => {
  return node.resourceName || node.name || node.resourceNo;
};

const getResourceTypeValue = (node: PermissionResourceNode) => {
  return String(node.resourceType ?? node.type ?? '').toUpperCase();
};

const getResourceTypeLabel = (node: PermissionResourceNode) => {
  const label = String(node.label || '');
  if (label === 'perm:module:dict' || label === 'perm:module:file') {
    return '基础服务';
  }
  if (label.startsWith('perm:module:')) {
    return '模块';
  }
  const type = getResourceTypeValue(node);
  return typeLabelMap[type] || '资源';
};

const getResourcePath = (node: PermissionResourceNode) => node.requestPath || node.path || node.label || '';

const getResourceTagType = (node: PermissionResourceNode) => {
  const label = getResourceTypeLabel(node);
  if (label === '模块') return 'primary';
  if (label === '基础服务') return 'success';
  if (label === '目录') return 'info';
  if (label === '菜单') return 'primary';
  if (label === '按钮' || label === '按钮/接口') return 'warning';
  if (label === '接口') return 'success';
  return 'info';
};

const collectResourceNos = (nodes: PermissionResourceNode[]) => {
  const result: string[] = [];
  const walk = (items: PermissionResourceNode[]) => {
    items.forEach((item) => {
      if (item.resourceNo) {
        result.push(item.resourceNo);
      }
      if (item.children?.length) {
        walk(item.children);
      }
    });
  };
  walk(nodes);
  return result;
};

const collectLeafResourceNos = (nodes: PermissionResourceNode[]) => {
  const result = new Set<string>();
  const walk = (items: PermissionResourceNode[]) => {
    items.forEach((item) => {
      if (item.children?.length) {
        walk(item.children);
        return;
      }
      if (item.resourceNo) {
        result.add(item.resourceNo);
      }
    });
  };
  walk(nodes);
  return result;
};

const normalizeResourceNos = (data: unknown): string[] => {
  if (Array.isArray(data)) {
    return data
      .map((item: any) => typeof item === 'string' ? item : item?.resourceNo)
      .filter((item: unknown): item is string => typeof item === 'string' && item.length > 0);
  }
  if (data && typeof data === 'object' && Array.isArray((data as any).resourceNos)) {
    return normalizeResourceNos((data as any).resourceNos);
  }
  return [];
};

const fetchRoles = async () => {
  try {
    roleLoading.value = true;
    const res: any = await getPermissionRolesAPI();
    if (res.code !== 200) {
      throw new Error(res.message || '加载角色列表失败');
    }
    roleList.value = Array.isArray(res.data) ? res.data : [];
    if (!selectedRoleId.value && roleList.value.length > 0) {
      await selectRole(roleList.value[0]);
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载角色列表失败');
  } finally {
    roleLoading.value = false;
  }
};

const fetchResourceTree = async () => {
  const res: any = await getPermissionResourceTreeAPI();
  if (res.code !== 200) {
    throw new Error(res.message || '加载资源树失败');
  }
  resourceTree.value = Array.isArray(res.data) ? res.data : [];
};

const loadRoleResources = async (roleId: number | string) => {
  const res: any = await getRoleResourceNosAPI(roleId);
  if (res.code !== 200) {
    throw new Error(res.message || '加载角色授权失败');
  }
  await nextTick();
  const leafResourceNos = collectLeafResourceNos(resourceTree.value);
  const checkedLeafNos = normalizeResourceNos(res.data).filter((resourceNo) => leafResourceNos.has(resourceNo));
  resourceTreeRef.value?.setCheckedKeys(checkedLeafNos, true);
};

const selectRole = async (role: PermissionRole) => {
  const roleId = getRoleId(role);
  if (!roleId) {
    return;
  }
  selectedRoleId.value = roleId;
  selectedRoleName.value = getRoleName(role);
  await reloadCurrentRole();
};

const reloadCurrentRole = async () => {
  if (!selectedRoleId.value) {
    return;
  }
  try {
    resourceLoading.value = true;
    if (resourceTree.value.length === 0) {
      await fetchResourceTree();
    }
    await loadRoleResources(selectedRoleId.value);
  } catch (error: any) {
    ElMessage.error(error?.message || '加载授权信息失败');
  } finally {
    resourceLoading.value = false;
  }
};

const checkAllResources = () => {
  resourceTreeRef.value?.setCheckedKeys(collectResourceNos(resourceTree.value), false);
};

const clearCheckedResources = () => {
  resourceTreeRef.value?.setCheckedKeys([], false);
};

const handleSave = async () => {
  if (!canSave.value) {
    ElMessage.warning('当前账号无保存授权权限');
    return;
  }
  if (!selectedRoleId.value) {
    ElMessage.warning('请先选择角色');
    return;
  }
  try {
    saveLoading.value = true;
    const checkedKeys = resourceTreeRef.value?.getCheckedKeys(false) || [];
    const halfCheckedKeys = resourceTreeRef.value?.getHalfCheckedKeys() || [];
    const resourceNos = Array.from(new Set([...checkedKeys, ...halfCheckedKeys].map((item) => String(item))));
    const res: any = await updateRoleResourceNosAPI(selectedRoleId.value, resourceNos);
    if (res.code !== 200) {
      throw new Error(res.message || '保存授权失败');
    }
    await userStore.syncCurrentUserInfo();
    ElMessage.success('授权保存成功');
  } catch (error: any) {
    ElMessage.error(error?.message || '保存授权失败');
  } finally {
    saveLoading.value = false;
  }
};

onMounted(async () => {
  if (!canAccess.value) {
    return;
  }
  try {
    resourceLoading.value = true;
    await fetchResourceTree();
  } catch (error: any) {
    ElMessage.error(error?.message || '加载资源树失败');
  } finally {
    resourceLoading.value = false;
  }
  await fetchRoles();
});
</script>

<style scoped>
.permission-page {
  --pm-page-bg: #f3f6fa;
  --pm-bg-card: #ffffff;
  --pm-border: #d7e0ea;
  --pm-text-main: #1a2f4d;
  --pm-text-sub: #5d728f;
  --pm-accent: #3b6fb1;
  --pm-shadow: 0 2px 8px rgba(26, 47, 77, 0.06);
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
  box-sizing: border-box;
  padding: 10px 12px 14px;
  background: var(--pm-page-bg);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 96px;
  padding: 16px 18px;
  border-radius: 8px;
  border: 1px solid #d4dde8;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  box-shadow: var(--pm-shadow);
}

.page-title {
  margin: 0;
  color: var(--pm-text-main);
  font-size: clamp(22px, 1.8vw, 26px);
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: 0;
}

.page-desc {
  margin: 10px 0 0;
  color: var(--pm-text-sub);
  font-size: 13px;
  line-height: 1.5;
  padding-top: 10px;
  border-top: 1px solid #e7edf4;
}

.save-btn {
  min-width: 118px;
  height: 40px;
  border-radius: 8px;
  border-color: var(--pm-accent);
  background: var(--pm-accent);
  box-shadow: none;
  font-weight: 600;
}

.permission-layout {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(0, 1fr);
  gap: 10px;
  min-height: calc(100vh - 190px);
}

.role-card,
.resource-card {
  border-radius: 8px;
  border: 1px solid var(--pm-border);
  background: var(--pm-bg-card);
  box-shadow: var(--pm-shadow);
}

.role-card :deep(.el-card__header),
.resource-card :deep(.el-card__header) {
  padding: 14px 18px;
  border-bottom: 1px solid #e3eaf2;
  background: #fbfcfe;
}

.role-card :deep(.el-card__body),
.resource-card :deep(.el-card__body) {
  padding: 16px 18px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--pm-text-main);
  font-weight: 700;
}

.card-header small {
  display: block;
  margin-top: 4px;
  color: var(--pm-text-sub);
  font-size: 12px;
  font-weight: 400;
}

.role-list {
  min-height: 260px;
}

.role-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  width: 100%;
  padding: 14px 12px;
  margin-bottom: 8px;
  border: 1px solid #d7e0ea;
  border-radius: 8px;
  background: #ffffff;
  color: var(--pm-text-main);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.role-item:hover,
.role-item.active {
  border-color: #3b6fb1;
  background: #f5f8fc;
}

.role-name {
  font-size: 15px;
  font-weight: 700;
}

.role-code {
  color: var(--pm-text-sub);
  font-size: 12px;
}

.tree-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.tree-actions :deep(.el-button) {
  margin-left: 0;
  border-radius: 4px;
}

.tree-panel {
  min-height: calc(100vh - 300px);
  padding: 4px 0;
}

.resource-tree {
  color: var(--pm-text-main);
}

.resource-tree :deep(.el-tree) {
  width: 100%;
}

.resource-tree :deep(> .el-tree-node) {
  display: inline-block;
  width: calc(50% - 8px);
  min-width: 420px;
  vertical-align: top;
}

.resource-tree :deep(> .el-tree-node:nth-child(odd)) {
  margin-right: 16px;
}

.resource-tree :deep(.el-tree-node__content) {
  min-height: 42px;
  border-radius: 6px;
  margin-bottom: 4px;
  border: 1px solid transparent;
}

.resource-tree :deep(.el-tree-node__content:hover) {
  border-color: #e3eaf2;
  background: #f8fbff;
}

.resource-tree :deep(.el-checkbox) {
  margin-right: 8px;
}

.resource-tree :deep(.el-tree-node__expand-icon) {
  color: #8fa1ba;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  padding: 0 8px 0 2px;
}

.node-title {
  min-width: 0;
  max-width: 220px;
  overflow: hidden;
  color: #17345c;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}

.resource-type-tag {
  width: fit-content;
  flex-shrink: 0;
  border-radius: 4px;
  background: #f6f8fb;
}

.node-path {
  min-width: 0;
  max-width: min(30vw, 360px);
  color: var(--pm-text-sub);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: "Consolas", "Monaco", monospace;
}

.node-path.muted {
  color: #a8b4c4;
  font-family: inherit;
}

.mb-16 {
  margin-bottom: 16px;
}

@media (max-width: 992px) {
  .permission-page {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    padding: 16px;
  }

  .save-btn {
    width: 100%;
  }

  .permission-layout {
    grid-template-columns: 1fr;
  }

  .resource-tree :deep(> .el-tree-node) {
    display: block;
    width: 100%;
    min-width: 0;
  }

  .resource-tree :deep(> .el-tree-node:nth-child(odd)) {
    margin-right: 0;
  }

  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .tree-actions {
    justify-content: flex-start;
  }

  .tree-node {
    max-width: 100%;
  }

  .node-path {
    max-width: 180px;
  }
}
</style>
