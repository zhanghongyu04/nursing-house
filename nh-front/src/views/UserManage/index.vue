<template>
  <div class="user-management-page">
    <el-alert
      v-if="!canAccess"
      title="当前账号无权访问用户管理"
      type="error"
      show-icon
      :closable="false"
      description="请在权限树中授予用户分页查询资源后再访问。"
      class="mb-16"
    />

    <template v-else>
      <div class="page-header">
        <div>
          <h2 class="page-title">用户管理</h2>
          <p class="page-desc">支持用户查询、新增、编辑、删除、重置密码与机构授权范围维护。</p>
        </div>
        <el-button v-if="canCreateUser" class="create-btn" type="primary" :icon="Plus" @click="openCreateDialog">新增用户</el-button>
      </div>

      <el-card shadow="never" class="query-card">
        <el-form class="query-form" :model="queryForm" inline>
          <el-form-item label="用户名">
            <el-input
              v-model.trim="queryForm.username"
              placeholder="请输入用户名"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item v-if="hasGlobalSanaScope" label="所属机构">
            <el-select v-model="queryForm.sanaId" placeholder="全部机构" clearable filterable style="width: 260px">
              <el-option
                v-for="item in sanatoriumOptions"
                :key="item.id"
                :label="item.sanaName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button class="search-btn" type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button class="reset-filter-btn" :icon="Refresh" @click="handleResetSearch">清空筛选</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="table-card">
        <el-table class="user-table" :data="tableData" border stripe v-loading="loading">
          <el-table-column type="index" label="#" width="60" :resizable="false" />
          <el-table-column prop="username" label="用户名" min-width="140" :resizable="false" />
          <el-table-column prop="email" label="邮箱" min-width="200" :resizable="false" />
          <el-table-column prop="phoneNumber" label="手机号" min-width="140" :resizable="false" />
          <el-table-column label="所属机构" min-width="220" :resizable="false">
            <template #default="{ row }">
              <el-tag class="sana-tag" type="info" :title="getSanaDisplayText(row)">{{ getSanaDisplayText(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170" :resizable="false">
            <template #default="{ row }">
              {{ formatTimeToMinute(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" :resizable="false">
            <template #default="{ row }">
              <el-space :size="8">
                <el-button v-if="canUpdateUser" class="op-btn edit-btn" size="small" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
                <el-button
                  v-if="canResetUserPassword"
                  class="op-btn reset-pwd-btn"
                  size="small"
                  type="warning"
                  :icon="RefreshRight"
                  :disabled="isCurrentUser(row.username)"
                  @click="handleResetPassword(row.username)"
                >重置密码</el-button>
                <el-button
                  v-if="canDeleteUser"
                  class="op-btn delete-btn"
                  size="small"
                  type="danger"
                  :icon="Delete"
                  :disabled="isCurrentUser(row.username)"
                  @click="handleDeleteUser(row.username)"
                >删除</el-button>
              </el-space>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap" v-if="total > 0">
          <el-pagination
            v-model:current-page="pageQuery.page"
            v-model:page-size="pageQuery.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>

      <el-dialog
        class="user-dialog"
        v-model="dialogVisible"
        :title="dialogMode === 'create' ? '新增用户' : '编辑用户'"
        width="640px"
        destroy-on-close
      >
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
          <el-form-item v-if="dialogMode === 'create'" label="用户名" prop="username">
            <el-input v-model.trim="formData.username" maxlength="30" show-word-limit placeholder="请输入用户名" />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model.trim="formData.email" maxlength="100" placeholder="请输入邮箱" />
          </el-form-item>

          <el-form-item label="手机号" prop="phoneNumber">
            <el-input v-model.trim="formData.phoneNumber" maxlength="11" placeholder="请输入手机号" />
          </el-form-item>

          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model.trim="formData.password"
              type="password"
              show-password
              maxlength="50"
              placeholder="请输入登录密码"
            />
            <div class="field-tip">
              {{ dialogMode === 'create' ? '可选填写登录密码；留空则默认密码为 admin123。' : '编辑时留空表示不修改密码。' }}
            </div>
          </el-form-item>

          <el-form-item label="用户头像" prop="avatar">
            <div class="avatar-upload-wrap">
              <el-upload
                :show-file-list="false"
                :auto-upload="false"
                accept="image/*"
                :disabled="avatarUploading"
                @change="handleAvatarSelect"
              >
                <el-button :loading="avatarUploading">选择本地图片</el-button>
              </el-upload>
              <el-avatar :size="48" :src="buildAvatarPreviewSrc(formData.avatar) || undefined">
                <span>头像</span>
              </el-avatar>
            </div>
            <div class="field-tip">仅支持本地图片上传到 RustFS，支持 JPG/PNG/GIF/WebP，大小不超过 5MB。</div>
          </el-form-item>

          <el-form-item v-if="dialogMode === 'edit'" label="权限变更">
            <el-switch
              v-model="editAdvancedEnabled"
              active-text="启用角色与机构授权编辑"
              inactive-text="仅更新邮箱和手机号"
            />
          </el-form-item>

          <el-form-item
            label="用户角色"
            prop="roleId"
            :required="dialogMode === 'create' || editAdvancedEnabled"
          >
            <el-select
              v-model="formData.roleId"
              placeholder="请选择角色"
              style="width: 100%"
              :disabled="dialogMode === 'edit' && !editAdvancedEnabled"
            >
              <el-option
                v-for="role in roleOptions"
                :key="role.id"
                :label="role.name"
                :value="role.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item
            label="机构授权范围"
            prop="sanaScopeIds"
            :required="scopeRequired"
          >
            <el-select
              v-model="formData.sanaScopeIds"
              multiple
              filterable
              clearable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择机构，可多选"
              style="width: 100%"
              :disabled="dialogMode === 'edit' && !editAdvancedEnabled"
            >
              <el-option
                v-for="item in availableScopeOptions"
                :key="item.id"
                :label="item.sanaName"
                :value="item.id"
              />
            </el-select>
            <div class="field-tip">子机构管理员/母机构管理员/护理人员必须绑定至少一个机构。</div>
          </el-form-item>

          <el-alert
            v-if="dialogMode === 'edit'"
            type="info"
            show-icon
            :closable="false"
            title="编辑说明"
            description="默认仅更新邮箱和手机号。开启“权限变更”后才会修改角色与机构授权。"
          />
        </el-form>

        <template #footer>
          <el-button class="dialog-cancel-btn" @click="dialogVisible = false">取消</el-button>
          <el-button class="dialog-confirm-btn" type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules, UploadFile } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Edit, Plus, Refresh, RefreshRight, Search } from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/userStore';
import { formatTimeToMinute } from '@/utils/timeParser.ts';
import { getSanatoriumPage } from '@/api/sanatorium.ts';
import { buildImageProxySrc, upload } from '@/api/file.ts';
import { hasResourcePath } from '@/constants/authRoles.ts';
import {
  addUserAPI,
  deleteUserAPI,
  getUserPageAPI,
  resetUserPasswordAPI,
  updateUserInfoAPI,
  type UserManageDTO,
  type UserManagePageQuery,
} from '@/api/admin.ts';

interface UserRow {
  id: number;
  username: string;
  email?: string;
  phoneNumber?: string;
  avatar?: string;
  sanaId?: number;
  sanaScopeIds?: number[];
  roleId?: number;
  createTime?: string;
}

interface SanatoriumOption {
  id: number;
  sanaName: string;
}

interface RoleOption {
  id: number;
  code: string;
  name: string;
  assignPath: string;
}

const ROLE_MAP: RoleOption[] = [
  { id: 2, code: 'GOV_ADMIN', name: '政府管理员', assignPath: '/web/admin/assign-role/gov-admin' },
  { id: 5, code: 'PARENT_ORG_ADMIN', name: '母机构管理员', assignPath: '/web/admin/assign-role/parent-admin' },
  { id: 3, code: 'ORG_ADMIN', name: '子机构管理员', assignPath: '/web/admin/assign-role/org-admin' },
  { id: 4, code: 'NURSE', name: '护理人员', assignPath: '/web/admin/assign-role/nurse' },
];

const DEFAULT_ROLE_CODES = ['PARENT_ORG_ADMIN', 'ORG_ADMIN', 'NURSE', 'GOV_ADMIN'];
const userStore = useUserStore();
const hasGlobalSanaScope = computed(() => {
  return !userStore.userInfo.sanaId && (userStore.userInfo.sanaScopeIds || []).length === 0;
});
const canAccess = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/admin/pageUser'));
const canCreateUser = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/admin/add'));
const canUpdateUser = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/admin/update'));
const canDeleteUser = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/admin/delete'));
const canResetUserPassword = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/admin/resetPassword'));

const queryForm = reactive({
  username: '',
  sanaId: null as number | null,
});

const pageQuery = reactive({
  page: 1,
  pageSize: 10,
});

const tableData = ref<UserRow[]>([]);
const total = ref(0);
const loading = ref(false);

const sanatoriumOptions = ref<SanatoriumOption[]>([]);
const sanaNameMap = computed(() => {
  const map = new Map<number, string>();
  sanatoriumOptions.value.forEach((item) => map.set(item.id, item.sanaName));
  return map;
});

const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'edit'>('create');
const editAdvancedEnabled = ref(false);
const submitLoading = ref(false);
const avatarUploading = ref(false);
const formRef = ref<FormInstance>();

const formData = reactive<UserManageDTO>({
  id: undefined,
  username: '',
  password: '',
  email: '',
  phoneNumber: '',
  avatar: '',
  roleId: undefined,
  sanaId: null,
  sanaScopeIds: [],
});

const roleOptions = computed<RoleOption[]>(() => {
  return ROLE_MAP.filter((item) => hasResourcePath(userStore.userInfo.resourcePaths, item.assignPath));
});

const defaultRoleOption = computed(() => {
  return DEFAULT_ROLE_CODES
    .map((code) => roleOptions.value.find((item) => item.code === code))
    .find((item): item is RoleOption => Boolean(item));
});

const availableScopeOptions = computed(() => sanatoriumOptions.value);

const requiresScopeByRole = (roleId?: number) => {
  if (!roleId) {
    return false;
  }
  const role = ROLE_MAP.find((item) => item.id === roleId);
  return role?.code === 'PARENT_ORG_ADMIN' || role?.code === 'ORG_ADMIN' || role?.code === 'NURSE';
};

const scopeRequired = computed(() => {
  if (dialogMode.value === 'create') {
    return requiresScopeByRole(formData.roleId);
  }
  if (dialogMode.value === 'edit' && editAdvancedEnabled.value) {
    return requiresScopeByRole(formData.roleId);
  }
  return false;
});

const formRules: FormRules<UserManageDTO> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '用户名长度在 3~30 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] },
  ],
  phoneNumber: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: ['blur', 'change'] },
  ],
  password: [
    {
      validator: (_rule, value: string | undefined, callback) => {
        const pwd = value?.trim() || '';
        if (pwd && pwd.length < 6) {
          callback(new Error('登录密码长度不能少于6位'));
          return;
        }
        callback();
      },
      trigger: ['blur', 'change'],
    },
  ],
  avatar: [
    { required: true, message: '请上传头像图片', trigger: 'change' },
    { min: 5, message: '头像上传失败，请重新上传', trigger: 'change' },
  ],
  roleId: [
    {
      validator: (_rule, value, callback) => {
        if (dialogMode.value === 'edit' && !editAdvancedEnabled.value) {
          callback();
          return;
        }
        if (!value) {
          callback(new Error('请选择角色'));
          return;
        }
        callback();
      },
      trigger: 'change',
    },
  ],
  sanaScopeIds: [
    {
      validator: (_rule, value: number[] | undefined, callback) => {
        if (!scopeRequired.value) {
          callback();
          return;
        }
        if (!value || value.length === 0) {
          callback(new Error('请至少选择一个机构'));
          return;
        }
        callback();
      },
      trigger: 'change',
    },
  ],
};

watch(editAdvancedEnabled, (enabled) => {
  if (!enabled && dialogMode.value === 'edit') {
    formData.roleId = undefined;
    formData.sanaScopeIds = [];
    formData.sanaId = null;
  }
});

watch(
  () => formData.sanaScopeIds,
  (value) => {
    if (!value || value.length === 0) {
      formData.sanaId = null;
      return;
    }
    formData.sanaId = value[0];
  },
  { deep: true },
);

onMounted(async () => {
  if (!canAccess.value) {
    return;
  }
  await Promise.all([fetchSanatoriumOptions(), fetchUserList()]);
});

const fetchSanatoriumOptions = async () => {
  try {
    const pageSize = 200;
    let page = 1;
    let totalCount = 0;
    const list: SanatoriumOption[] = [];

    do {
      const res = await getSanatoriumPage({ page, pageSize });
      const records = res?.data?.records || [];
      totalCount = Number(res?.data?.total || 0);
      records.forEach((item: any) => {
        if (typeof item.id === 'number') {
          list.push({ id: item.id, sanaName: item.sanaName || `机构#${item.id}` });
        }
      });
      page += 1;
    } while (list.length < totalCount && totalCount > 0);

    const dedup = new Map<number, SanatoriumOption>();
    list.forEach((item) => dedup.set(item.id, item));
    sanatoriumOptions.value = Array.from(dedup.values());
  } catch (error) {
    console.error('加载机构列表失败', error);
    ElMessage.warning('机构列表加载失败，机构名称将以ID显示');
  }
};

const buildPageQuery = (): UserManagePageQuery => {
  const query: UserManagePageQuery = {
    page: pageQuery.page,
    pageSize: pageQuery.pageSize,
  };
  if (queryForm.username) {
    query.username = queryForm.username;
  }
  if (hasGlobalSanaScope.value && queryForm.sanaId) {
    query.sanaId = queryForm.sanaId;
  }
  return query;
};

const fetchUserList = async () => {
  try {
    loading.value = true;
    const res = await getUserPageAPI(buildPageQuery());
    if (res.code !== 200) {
      throw new Error(res.message || '查询失败');
    }
    tableData.value = (res.data?.records || []) as UserRow[];
    total.value = Number(res.data?.total || 0);
  } catch (error: any) {
    console.error('加载用户列表失败', error);
    ElMessage.error(error?.message || '加载用户列表失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  pageQuery.page = 1;
  await fetchUserList();
};

const handleResetSearch = async () => {
  queryForm.username = '';
  queryForm.sanaId = null;
  pageQuery.page = 1;
  await fetchUserList();
};

const handleSizeChange = async (size: number) => {
  pageQuery.pageSize = size;
  pageQuery.page = 1;
  await fetchUserList();
};

const handleCurrentChange = async (page: number) => {
  pageQuery.page = page;
  await fetchUserList();
};

const resetFormData = () => {
  formData.id = undefined;
  formData.username = '';
  formData.password = '';
  formData.email = '';
  formData.phoneNumber = '';
  formData.avatar = '';
  formData.roleId = undefined;
  formData.sanaId = null;
  formData.sanaScopeIds = [];
};

const openCreateDialog = () => {
  if (!canCreateUser.value) {
    ElMessage.warning('当前账号无新增用户权限');
    return;
  }
  dialogMode.value = 'create';
  editAdvancedEnabled.value = true;
  resetFormData();
  formData.roleId = defaultRoleOption.value?.id;
  dialogVisible.value = true;
};

const openEditDialog = (row: UserRow) => {
  if (!canUpdateUser.value) {
    ElMessage.warning('当前账号无编辑用户权限');
    return;
  }
  dialogMode.value = 'edit';
  editAdvancedEnabled.value = false;
  resetFormData();
  formData.id = row.id;
  formData.username = row.username;
  formData.password = '';
  formData.email = row.email || '';
  formData.phoneNumber = row.phoneNumber || '';
  formData.avatar = row.avatar || '';
  dialogVisible.value = true;
};

const buildSubmitPayload = (): UserManageDTO => {
  const payload: UserManageDTO = {
    id: formData.id,
    username: formData.username,
    email: formData.email,
    phoneNumber: formData.phoneNumber,
    avatar: formData.avatar,
  };
  const passwordValue = formData.password?.trim() || '';
  if (dialogMode.value === 'create' || passwordValue) {
    payload.password = passwordValue;
  }

  if (dialogMode.value === 'create') {
    payload.roleId = formData.roleId;
    payload.sanaScopeIds = [...(formData.sanaScopeIds || [])];
    payload.sanaId = payload.sanaScopeIds.length > 0 ? payload.sanaScopeIds[0] : null;
    return payload;
  }

  if (editAdvancedEnabled.value) {
    payload.roleId = formData.roleId;
    payload.sanaScopeIds = [...(formData.sanaScopeIds || [])];
    payload.sanaId = payload.sanaScopeIds.length > 0 ? payload.sanaScopeIds[0] : null;
  }

  return payload;
};

const handleSubmit = async () => {
  if (!formRef.value) {
    return;
  }
  try {
    if (dialogMode.value === 'create' && !canCreateUser.value) {
      ElMessage.warning('当前账号无新增用户权限');
      return;
    }
    if (dialogMode.value === 'edit' && !canUpdateUser.value) {
      ElMessage.warning('当前账号无编辑用户权限');
      return;
    }
    if (avatarUploading.value) {
      ElMessage.warning('头像上传中，请稍候再提交');
      return;
    }
    await formRef.value.validate();
    submitLoading.value = true;

    const useDefaultPassword = dialogMode.value === 'create' && !(formData.password?.trim());
    const payload = buildSubmitPayload();

    const res = dialogMode.value === 'create'
      ? await addUserAPI(payload)
      : await updateUserInfoAPI(payload);

    if (res.code !== 200 || !res.data) {
      throw new Error(res.message || (dialogMode.value === 'create' ? '新增失败' : '更新失败'));
    }

    ElMessage.success(
      dialogMode.value === 'create'
        ? (useDefaultPassword ? '新增用户成功，默认密码为 admin123' : '新增用户成功')
        : '更新用户成功'
    );
    dialogVisible.value = false;
    await fetchUserList();
  } catch (error: any) {
    if (error?.message) {
      ElMessage.error(error.message);
    }
  } finally {
    submitLoading.value = false;
  }
};

const handleResetPassword = async (username: string) => {
  if (!canResetUserPassword.value) {
    ElMessage.warning('当前账号无重置密码权限');
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认重置用户 ${username} 的密码为默认值 admin123 吗？`,
      '重置密码',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      },
    );

    const res = await resetUserPasswordAPI(username);
    if (res.code !== 200 || !res.data) {
      throw new Error(res.message || '重置失败');
    }

    ElMessage.success('重置成功，默认密码为 admin123');
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '重置密码失败');
    }
  }
};

const handleDeleteUser = async (username: string) => {
  if (!canDeleteUser.value) {
    ElMessage.warning('当前账号无删除用户权限');
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认删除用户 ${username} 吗？删除后不可恢复。`,
      '删除用户',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      },
    );

    const res = await deleteUserAPI(username);
    if (res.code !== 200 || !res.data) {
      throw new Error(res.message || '删除失败');
    }

    ElMessage.success('用户删除成功');
    if (tableData.value.length === 1 && pageQuery.page > 1) {
      pageQuery.page -= 1;
    }
    await fetchUserList();
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '删除用户失败');
    }
  }
};

const handleAvatarSelect = async (uploadFile: UploadFile) => {
  const file = uploadFile.raw;
  if (!file) {
    ElMessage.error('读取上传文件失败，请重试');
    return;
  }

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件');
    return;
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('头像图片不能超过 5MB');
    return;
  }

  try {
    avatarUploading.value = true;
    const form = new FormData();
    form.append('file', file);
    const res = await upload(form);
    if (res.code !== 200 || !res.data) {
      throw new Error(res.message || '头像上传失败');
    }
    formData.avatar = res.data;
    formRef.value?.validateField('avatar');
    ElMessage.success('头像上传成功');
  } catch (error: any) {
    ElMessage.error(error?.message || '头像上传失败');
  } finally {
    avatarUploading.value = false;
  }
};

const buildAvatarPreviewSrc = (avatarUrl?: string) => {
  return buildImageProxySrc(avatarUrl || '');
};

const getSanaName = (sanaId?: number) => {
  if (!sanaId) {
    return '--';
  }
  return sanaNameMap.value.get(sanaId) || `机构#${sanaId}`;
};

const getSanaDisplayText = (row: UserRow) => {
  const scopeIds = (row.sanaScopeIds || []).filter((id) => Number.isFinite(id));
  if (scopeIds.length > 0) {
    return scopeIds.map((id) => getSanaName(id)).join('、');
  }
  return getSanaName(row.sanaId);
};

const isCurrentUser = (username: string) => {
  return username === userStore.userInfo.username;
};
</script>

<style scoped>
.user-management-page {
  --um-page-bg: #f3f6fa;
  --um-bg-card: #ffffff;
  --um-border: #d7e0ea;
  --um-text-main: #1a2f4d;
  --um-text-sub: #5d728f;
  --um-accent: #2f5e9e;
  --um-accent-strong: #3b6fb1;
  --um-shadow: 0 2px 8px rgba(26, 47, 77, 0.06);
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
  box-sizing: border-box;
  padding: 10px 12px 14px;
  background: var(--um-page-bg);
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
  box-shadow: var(--um-shadow);
}

.page-title {
  margin: 0;
  color: var(--um-text-main);
  font-size: clamp(22px, 1.8vw, 26px);
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: 0;
}

.page-desc {
  margin: 10px 0 0;
  color: var(--um-text-sub);
  font-size: 13px;
  line-height: 1.5;
  padding-top: 10px;
  border-top: 1px solid #e7edf4;
}

.create-btn {
  min-width: 118px;
  height: 40px;
  border-radius: 8px;
  border-color: var(--um-accent-strong);
  background: var(--um-accent-strong);
  box-shadow: none;
  font-weight: 600;
}

.query-card,
.table-card {
  border-radius: 8px;
  border: 1px solid var(--um-border);
  background: var(--um-bg-card);
  box-shadow: var(--um-shadow);
}

.query-card :deep(.el-card__body),
.table-card :deep(.el-card__body) {
  padding: 18px 22px;
}

.query-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 10px;
}

.query-form :deep(.el-input__wrapper),
.query-form :deep(.el-select__wrapper) {
  border-radius: 4px;
  border: 1px solid #d7e0ea;
  box-shadow: none;
  background: #ffffff;
  min-height: 40px;
}

.search-btn,
.reset-filter-btn {
  min-width: 100px;
  height: 40px;
  border-radius: 4px;
  font-weight: 500;
}

.search-btn {
  border-color: var(--um-accent-strong);
  background: var(--um-accent-strong);
}

.reset-filter-btn {
  border-color: #d7e0ea;
  color: #526b8d;
  background: #f5f8fc;
}

.user-table :deep(.el-table__header th) {
  background: #f5f8fc !important;
  color: var(--um-text-main);
  font-weight: 700;
}

.user-table :deep(.el-table__row td) {
  color: var(--um-text-main);
}

.user-table :deep(.el-table__row:hover td) {
  background: #f8fafc !important;
}

.user-table :deep(.el-tag) {
  border-radius: 4px;
  border-color: #d6dee7;
  color: #526b8d;
  background: #f2f5f9;
}

.user-table :deep(.sana-tag) {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.user-management-page :deep(.el-select .el-tag),
.user-management-page :deep(.el-select__selection .el-tag),
.user-management-page :deep(.el-select-dropdown__item .el-tag) {
  border-radius: 4px;
  padding-left: 8px;
  padding-right: 8px;
}

.user-management-page :deep(.el-select .el-tag__close),
.user-management-page :deep(.el-select__selection .el-tag__close) {
  border-radius: 2px;
}

.op-btn {
  border-radius: 6px;
  font-weight: 500;
}

.edit-btn {
  border-color: #d7e0ea;
  color: #355783;
  background: #f5f8fc;
}

.reset-pwd-btn {
  border: none;
  background: #c59a0f;
}

.delete-btn {
  border: none;
  background: #d95f5f;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e2eaf4;
}

.pagination-wrap :deep(.el-pagination) {
  --el-pagination-button-bg-color: #f5f8fc;
  --el-pagination-hover-color: var(--um-accent);
}

.user-dialog :deep(.el-dialog) {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #d7e0ea;
  box-shadow: 0 8px 20px rgba(26, 47, 77, 0.14);
}

.user-dialog :deep(.el-dialog__header) {
  margin-right: 0;
  padding: 14px 18px 12px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  border-bottom: 1px solid #e3eaf2;
}

.user-dialog :deep(.el-dialog__title) {
  font-weight: 700;
  color: var(--um-text-main);
}

.user-dialog :deep(.el-dialog__body) {
  padding: 18px 20px 12px;
}

.dialog-cancel-btn,
.dialog-confirm-btn {
  min-width: 84px;
  border-radius: 4px;
  font-weight: 500;
}

.dialog-confirm-btn {
  border-color: var(--um-accent-strong);
  background: var(--um-accent-strong);
}

.field-tip {
  margin-top: 6px;
  color: var(--um-text-sub);
  font-size: 12px;
  line-height: 1.4;
}

.avatar-upload-wrap {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-upload-wrap :deep(.el-button) {
  border-radius: 4px;
  border-color: #d7e0ea;
  color: #526b8d;
  background: #f5f8fc;
}

.avatar-upload-wrap :deep(.el-avatar) {
  border: 1px solid #d7e0ea;
  background: #f2f5f9;
}

.mb-16 {
  margin-bottom: 16px;
}

@media (max-width: 992px) {
  .user-management-page {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    padding: 16px;
  }

  .create-btn {
    width: 100%;
  }

  .query-card :deep(.el-card__body),
  .table-card :deep(.el-card__body) {
    padding: 14px;
  }

  .query-form :deep(.el-form-item) {
    width: 100%;
    margin-right: 0;
  }

  .query-form :deep(.el-input),
  .query-form :deep(.el-select) {
    width: 100%;
  }

  .pagination-wrap {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
