<template>
  <section class="profile-page">
    <div class="profile-shell">
      <div class="hero-banner">
        <div class="hero-content">
          <h2>个人中心</h2>
          <p>管理个人资料、账户安全与系统偏好设置</p>
        </div>
      </div>

      <div class="profile-grid">
        <article class="panel panel-user">
          <div class="avatar-section">
            <el-avatar :size="100" :src="buildAvatarSrc(userInfo.avatar)" class="user-avatar">
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
            <div class="user-basic">
              <h3>{{ userInfo.username || '未登录' }}</h3>
              <p class="profile-badge">个人档案</p>
              <div class="user-contact">
                <span v-if="userInfo.email">{{ userInfo.email }}</span>
                <span v-if="userInfo.phoneNumber">{{ userInfo.phoneNumber }}</span>
              </div>
              <div class="completion-card">
                <div class="completion-head">
                  <span>资料完整度</span>
                  <strong>{{ profileCompletion }}%</strong>
                </div>
                <div class="completion-track">
                  <span class="completion-fill" :style="{ width: `${profileCompletion}%` }"></span>
                </div>
              </div>
              <div class="user-meta-list">
                <div class="meta-row">
                  <span class="meta-label">账户ID</span>
                  <span class="meta-value">{{ userInfo.userId || '--' }}</span>
                </div>
                <div class="meta-row">
                  <span class="meta-label">最近安全检查</span>
                  <span class="meta-value">已通过</span>
                </div>
                <div class="meta-row">
                  <span class="meta-label">账号状态</span>
                  <span class="meta-value status-ok">正常</span>
                </div>
              </div>
            </div>
          </div>
        </article>

        <article class="panel panel-settings">
          <el-tabs v-model="activeTab" class="settings-tabs">
            <el-tab-pane label="个人资料" name="profile">
              <div class="tab-pane-body">
                <el-form :model="profileForm" label-width="100px" class="profile-form">
                  <el-form-item label="用户名">
                    <el-input v-model="profileForm.username" placeholder="请输入用户名" />
                  </el-form-item>
                  <el-form-item label="手机号">
                    <el-input v-model="profileForm.phoneNumber" placeholder="请输入手机号" />
                  </el-form-item>
                  <el-form-item label="邮箱">
                    <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                  </el-form-item>
                  <el-form-item label="头像">
                    <div class="avatar-upload-row">
                      <el-avatar :size="48" :src="buildAvatarSrc(profileForm.avatar)" class="preview-avatar">
                        <el-icon><UserFilled /></el-icon>
                      </el-avatar>
                      <el-upload
                        :show-file-list="false"
                        :auto-upload="false"
                        accept="image/*"
                        :disabled="avatarUploading"
                        @change="handleAvatarChange"
                      >
                        <el-button :loading="avatarUploading" type="primary" plain>更换头像</el-button>
                      </el-upload>
                    </div>
                    <p class="upload-tip">支持 JPG/PNG/GIF/WebP，大小不超过 5MB</p>
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" :loading="profileLoading" @click="handleUpdateProfile">
                      保存修改
                    </el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <div class="tab-pane-body">
                <el-form :model="passwordForm" label-width="100px" class="password-form">
                  <el-form-item label="原密码">
                    <el-input
                      v-model="passwordForm.oldPassword"
                      type="password"
                      show-password
                      placeholder="请输入原密码"
                    />
                  </el-form-item>
                  <el-form-item label="新密码">
                    <el-input
                      v-model="passwordForm.newPassword"
                      type="password"
                      show-password
                      placeholder="请输入新密码（至少6位）"
                    />
                  </el-form-item>
                  <el-form-item label="确认密码">
                    <el-input
                      v-model="passwordForm.confirmPassword"
                      type="password"
                      show-password
                      placeholder="请再次输入新密码"
                    />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" :loading="passwordLoading" @click="handleUpdatePassword">
                      修改密码
                    </el-button>
                  </el-form-item>
                </el-form>
                <el-alert
                  type="info"
                  show-icon
                  :closable="false"
                  title="安全提示"
                  description="修改密码后需要重新登录，请确保新密码安全可靠。"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label="账户信息" name="account">
              <div class="tab-pane-body">
                <div class="account-info-list">
                  <div class="info-row">
                    <span class="info-label">账户ID</span>
                    <span class="info-value">{{ userInfo.userId || '--' }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">用户名</span>
                    <span class="info-value">{{ userInfo.username || '--' }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">手机号</span>
                    <span class="info-value">{{ userInfo.phoneNumber || '--' }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">邮箱</span>
                    <span class="info-value">{{ userInfo.email || '--' }}</span>
                  </div>
                  <div class="info-row" v-if="sanatoriumName">
                    <span class="info-label">所属机构</span>
                    <span class="info-value">{{ sanatoriumName }}</span>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { UserFilled } from '@element-plus/icons-vue';
import { getCurrentUserAPI, updateUserAPI, updatePasswordAPI } from '@/api/user';
import { buildImageProxySrc, upload } from '@/api/file';
import { getSanatoriumPage } from '@/api/sanatorium';
import { useUserStore } from '@/stores/userStore';

interface UserInfo {
  userId: number;
  username: string;
  avatar: string;
  sanaId: number | null;
  sanaName?: string;
  email: string;
  phoneNumber: string;
}

const buildAvatarSrc = (avatarUrl?: string) => buildImageProxySrc(avatarUrl || '');

const router = useRouter();
const userStore = useUserStore();
const userInfo = ref<UserInfo>({} as UserInfo);
const activeTab = ref('profile');
const avatarUploading = ref(false);
const profileLoading = ref(false);
const passwordLoading = ref(false);
const sanatoriumName = ref('');

const profileForm = reactive({
  username: '',
  phoneNumber: '',
  email: '',
  avatar: '',
});

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const profileCompletion = computed(() => {
  const fields = [
    Boolean(userInfo.value.username),
    Boolean(userInfo.value.phoneNumber),
    Boolean(userInfo.value.email),
    Boolean(userInfo.value.avatar),
  ];
  const completed = fields.filter(Boolean).length;
  return Math.round((completed / fields.length) * 100);
});

const fetchUserInfo = async () => {
  try {
    const res = await getCurrentUserAPI();
    if (res.code === 200 && res.data) {
      userInfo.value = res.data;
      profileForm.username = res.data.username || '';
      profileForm.phoneNumber = res.data.phoneNumber || '';
      profileForm.email = res.data.email || '';
      profileForm.avatar = res.data.avatar || '';
      const directSanaName = String(res.data.sanaName || '').trim();
      if (directSanaName) {
        sanatoriumName.value = directSanaName;
      } else if (res.data.sanaId) {
        sanatoriumName.value = await resolveSanatoriumNameById(res.data.sanaId);
      } else {
        sanatoriumName.value = '';
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
    ElMessage.error('获取用户信息失败');
  }
};

const resolveSanatoriumNameById = async (sanaId: number): Promise<string> => {
  try {
    const res = await getSanatoriumPage({ page: 1, pageSize: 200 });
    const records = res?.data?.records || [];
    const matched = records.find((item: any) => Number(item?.id) === Number(sanaId));
    return matched?.sanaName ? String(matched.sanaName) : '';
  } catch (error) {
    console.warn('查询机构名称失败:', error);
    return '';
  }
};

const handleAvatarChange = async (uploadFile: any) => {
  const file = uploadFile.raw;
  if (!file) return;

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件');
    return;
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB');
    return;
  }

  try {
    avatarUploading.value = true;
    const formData = new FormData();
    formData.append('file', file);
    const res = await upload(formData);
    if (res.code === 200 && res.data) {
      profileForm.avatar = res.data;
      ElMessage.success('头像上传成功');
    } else {
      ElMessage.error('头像上传失败');
    }
  } catch (error) {
    console.error('头像上传失败:', error);
    ElMessage.error('头像上传失败');
  } finally {
    avatarUploading.value = false;
  }
};

const handleUpdateProfile = async () => {
  try {
    profileLoading.value = true;
    const res = await updateUserAPI({
      userId: userInfo.value.userId,
      username: profileForm.username,
      phoneNumber: profileForm.phoneNumber,
      email: profileForm.email,
      avatar: profileForm.avatar,
    });
    if (res.code === 200) {
      ElMessage.success('个人资料更新成功');
      await fetchUserInfo();
    } else {
      ElMessage.error(res.msg || '更新失败');
    }
  } catch (error) {
    console.error('更新个人资料失败:', error);
    ElMessage.error('更新失败');
  } finally {
    profileLoading.value = false;
  }
};

const handleUpdatePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请填写完整的密码信息');
    return;
  }

  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位');
    return;
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致');
    return;
  }

  try {
    passwordLoading.value = true;
    const res = await updatePasswordAPI({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    });
    if (res.code === 200) {
      ElMessage.success('密码修改成功，请重新登录');
      passwordForm.oldPassword = '';
      passwordForm.newPassword = '';
      passwordForm.confirmPassword = '';
      userStore.clearUserInfo();
      router.replace('/login');
    } else {
      ElMessage.error(res.msg || '密码修改失败');
    }
  } catch (error) {
    console.error('修改密码失败:', error);
    ElMessage.error('密码修改失败');
  } finally {
    passwordLoading.value = false;
  }
};

onMounted(() => {
  fetchUserInfo();
});
</script>

<style scoped>
.profile-page {
  --page-bg: #f3f6fa;
  --panel-bg: #ffffff;
  --panel-border: #d7e0ea;
  --text-primary: #1a2f4d;
  --text-secondary: #5d728f;
  --accent: #2f5e9e;
  --accent-strong: #3b6fb1;
  --shadow-soft: 0 2px 8px rgba(26, 47, 77, 0.06);
  position: relative;
  isolation: isolate;
  background: var(--page-bg);
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
  padding: 10px 12px 14px;
  box-sizing: border-box;
}

.profile-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: -1;
  background:
    radial-gradient(circle at 8% 24%, rgba(77, 127, 196, 0.14) 0%, rgba(77, 127, 196, 0) 36%),
    radial-gradient(circle at 92% 16%, rgba(97, 150, 221, 0.12) 0%, rgba(97, 150, 221, 0) 34%),
    linear-gradient(180deg, #f8fbff 0%, #f3f6fa 45%, #eef3f9 100%);
}

.profile-shell {
  width: min(100%, 980px);
  max-width: calc(100vw - 24px);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hero-banner {
  border: 1px solid #d4dde8;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  border-radius: 8px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: var(--shadow-soft);
  min-height: 96px;
}

.hero-content h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: clamp(22px, 1.8vw, 26px);
  line-height: 1.25;
  font-weight: 700;
}

.hero-content p {
  margin: 10px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
  padding-top: 10px;
  border-top: 1px solid #e7edf4;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.4fr) minmax(0, 1fr);
  gap: 10px;
  align-items: stretch;
}

.panel {
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  box-shadow: var(--shadow-soft);
  height: 100%;
}

.panel-user {
  padding: 24px 20px;
}

.avatar-section {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.user-avatar {
  border: 2px solid #d7e0ea;
  background: #f2f5f9;
  box-shadow: 0 8px 20px rgba(36, 76, 132, 0.12);
}

.user-basic {
  width: 100%;
  max-width: 260px;
  text-align: center;
}

.user-basic h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 34px;
  font-weight: 700;
}

.profile-badge {
  margin: 8px auto 0;
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(59, 111, 177, 0.12);
  color: #2f5e9e;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.4px;
}

.user-contact {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #486485;
  font-size: 13px;
}

.completion-card {
  margin-top: 14px;
  padding: 10px 12px 12px;
  border: 1px solid #deebf9;
  border-radius: 10px;
  background: #ffffff;
}

.completion-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #4e6482;
  font-size: 12px;
}

.completion-head strong {
  color: #1f4f87;
  font-size: 15px;
}

.completion-track {
  margin-top: 8px;
  height: 8px;
  border-radius: 999px;
  background: #e4edf8;
  overflow: hidden;
}

.completion-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #5f97e7 0%, #2f6fd6 100%);
  transition: width 0.3s ease;
}

.user-meta-list {
  margin-top: 14px;
  padding: 12px 12px;
  border-radius: 10px;
  border: 1px solid #dde8f4;
  background: rgba(255, 255, 255, 0.92);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #e4ecf6;
}

.meta-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.meta-label {
  color: var(--text-secondary);
  font-size: 12px;
}

.meta-value {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.status-ok {
  color: #2f8f56;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid rgba(47, 143, 86, 0.25);
  background: rgba(47, 143, 86, 0.08);
}

.panel-settings {
  padding: 34px 16px 16px;
}

.settings-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.settings-tabs :deep(.el-tabs__header) {
  margin: 0 0 12px;
  padding-top: 2px;
}

.settings-tabs :deep(.el-tabs__item) {
  color: var(--text-secondary);
  font-weight: 500;
}

.settings-tabs :deep(.el-tabs__item.is-active) {
  color: var(--accent);
  font-weight: 600;
}

.settings-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--accent);
}

.settings-tabs :deep(.el-tabs__content) {
  padding-top: 2px;
  flex: 1;
  display: flex;
}

.settings-tabs :deep(.el-tab-pane) {
  width: 100%;
}

.settings-tabs :deep(.el-tab-pane.is-active) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tab-pane-body {
  max-width: 650px;
  border: 1px solid #e2e9f2;
  border-radius: 8px;
  margin-top: auto;
  margin-bottom: auto;
  padding: 22px 18px;
  background: #ffffff;
}

.profile-form,
.password-form {
  width: 100%;
  max-width: 560px;
}

.profile-form :deep(.el-form-item:last-child),
.password-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.profile-form :deep(.el-input__wrapper),
.password-form :deep(.el-input__wrapper) {
  border-radius: 4px;
  border: 1px solid #d7e0ea;
  box-shadow: none;
  background: #ffffff;
}

.profile-form :deep(.el-button--primary),
.password-form :deep(.el-button--primary) {
  min-width: 120px;
  border-radius: 4px;
  border-color: var(--accent-strong);
  background: var(--accent-strong);
  font-weight: 500;
}

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.preview-avatar {
  border: 1px solid #d7e0ea;
  background: #f2f5f9;
}

.avatar-upload-row :deep(.el-button) {
  border-radius: 4px;
}

.avatar-upload-row :deep(.el-button--primary.is-plain) {
  color: #ffffff;
  background: var(--accent-strong);
  border-color: var(--accent-strong);
}

.avatar-upload-row :deep(.el-button--primary.is-plain:hover),
.avatar-upload-row :deep(.el-button--primary.is-plain:focus-visible) {
  color: #ffffff;
  background: #2f5e9e;
  border-color: #2f5e9e;
}

.upload-tip {
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.account-info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 560px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px solid #d9e1ea;
  border-radius: 4px;
  background: #f8fafc;
}

.info-label {
  color: var(--text-secondary);
  font-size: 14px;
}

.info-value {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
}

.panel-settings :deep(.el-alert) {
  margin-top: 16px;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .profile-shell {
    width: calc(100vw - 16px);
  }

  .hero-banner {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 16px;
  }

  .profile-grid {
    grid-template-columns: 1fr;
  }

  .panel-user {
    padding: 20px 16px;
  }

  .user-basic {
    max-width: 100%;
  }

  .user-basic h3 {
    font-size: 28px;
  }

  .panel-settings {
    padding: 24px 14px 14px;
  }

  .tab-pane-body {
    max-width: 100%;
    margin-top: auto;
    margin-bottom: auto;
    padding: 18px 14px;
  }

  .profile-form,
  .password-form {
    max-width: 100%;
  }

  .avatar-upload-row {
    flex-wrap: wrap;
  }

  .info-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>
