<template>
  <header v-if="route.meta?.showNavbar !== false" class="nav-header">
    <div class="nav-brand">
      <img src="@/assets/logo/logo.png" alt="Logo" class="brand-logo" />
      <span class="brand-title">智慧康养管理系统</span>
    </div>

    <el-menu
      :default-active="activeMenu"
      mode="horizontal"
      class="nav-menu"
      :ellipsis="false"
      @select="handleMenuSelect"
    >
      <el-menu-item index="/">首页</el-menu-item>

      <el-sub-menu v-if="canManageSanatorium" index="/nursingHome" popper-class="nav-submenu-popper">
        <template #title>康养机构管理</template>
        <el-menu-item index="/nursingHomeList">康养机构信息页面</el-menu-item>
        <el-menu-item index="/nursingHomeDetail">康养机构详情页面</el-menu-item>
      </el-sub-menu>

      <el-menu-item v-if="canAccessElder" index="/elderInfo">老人信息</el-menu-item>

      <el-sub-menu v-if="canAccessSystemManage" index="/systemManage" popper-class="nav-submenu-popper">
        <template #title>系统管理</template>
        <el-menu-item v-if="canManageUsers" index="/userManage">用户管理</el-menu-item>
        <el-menu-item v-if="canAccessPermission" index="/permission">角色授权</el-menu-item>
        <el-menu-item v-if="canAccessCacheControl" index="/CacheControl">缓存监控</el-menu-item>
        <el-menu-item v-if="canAccessServiceControl" index="/ServiceControl">服务监控</el-menu-item>
        <el-menu-item v-if="canAccessLoginMonitor" index="/LoginMonitor">登录与在线监控</el-menu-item>
        <el-menu-item v-if="canAccessPromptConsole" index="/promptConsole">提示词控制台</el-menu-item>
      </el-sub-menu>

      <el-menu-item v-if="canManageVectorStore" index="/vectorStore">知识库管理</el-menu-item>

      <el-menu-item v-if="canAccessAgent" index="/agent">养护智能体</el-menu-item>
      <el-menu-item v-if="canAccessMonitor" index="/Monitor">视频实时监测</el-menu-item>

      <el-sub-menu v-if="canAccessNursingOrg" index="/nursingOrg" popper-class="nav-submenu-popper">
        <template #title>护理管理</template>
        <el-menu-item index="/nursingTaskDispatch">护理任务下发</el-menu-item>
        <el-menu-item index="/nursingTaskTemplate">任务模板</el-menu-item>
        <el-menu-item index="/nursingLog">护理日志</el-menu-item>
      </el-sub-menu>

      <el-sub-menu v-if="canAccessNursingPersonal" index="/nursingPersonal" popper-class="nav-submenu-popper">
        <template #title>我的护理</template>
        <el-menu-item v-if="canAccessMyNursingTask" index="/myNursingTask">我的任务</el-menu-item>
        <el-menu-item v-if="canAccessWriteNursingLog" index="/writeNursingLog">护理日志编写</el-menu-item>
      </el-sub-menu>
    </el-menu>

    <div class="nav-actions">
      <el-button
        circle
        text
        class="icon-button"
        aria-label="消息提醒"
      >
        <el-icon><Bell /></el-icon>
      </el-button>

      <el-button
        v-if="!userStore.userInfo.token"
        type="primary"
        plain
        @click="router.push('/login')"
      >
        用户登录
      </el-button>

      <el-dropdown v-else trigger="hover" placement="bottom-end">
        <div class="user-entry">
          <el-avatar :size="32" :src="userAvatarSrc">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <span class="user-name">{{ userStore.userInfo.username }}</span>
          <el-icon class="user-arrow"><ArrowDown /></el-icon>
        </div>

        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="router.push('/Personalcenter')">用户中心</el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ArrowDown, Bell, UserFilled } from "@element-plus/icons-vue";
import { ElMessageBox } from "element-plus";
import { useUserStore } from "@/stores/userStore";
import { buildImageProxySrc } from "@/api/file";
import { getCurrentUserAPI } from "@/api/user";
import { AUTH_ROLES, hasAnyRole, hasResourcePath } from "@/constants/authRoles";

const userStore = useUserStore();
const router = useRouter();
const route = useRoute();
const enablePromptConsole = import.meta.env.VITE_ENABLE_PROMPT_CONSOLE === "true";

const canManageUsers = computed(() => {
  return hasResourcePath(userStore.userInfo.resourcePaths, "/web/admin/pageUser");
});

const canManageSanatorium = computed(() => {
  return hasResourcePath(userStore.userInfo.resourcePaths, ["/web/sanatorium/page", "/web/sanatorium/elderDistribution"]);
});

const canAccessElder = computed(() => {
  return hasResourcePath(userStore.userInfo.resourcePaths, "/web/elder/page");
});

const canAccessPermission = computed(() => {
  return hasResourcePath(userStore.userInfo.resourcePaths, "/web/permission/roles");
});

const canAccessCacheControl = computed(() => {
  return hasResourcePath(userStore.userInfo.resourcePaths, "/web/monitor/getRedisInfo");
});

const canAccessServiceControl = computed(() => {
  return hasResourcePath(userStore.userInfo.resourcePaths, "/web/monitor/serverMonitor");
});

const canAccessLoginMonitor = computed(() => {
  return hasResourcePath(userStore.userInfo.resourcePaths, ["/web/monitor/loginLogs", "/web/monitor/onlineUsers"]);
});

const canAccessPromptConsole = computed(() => {
  return enablePromptConsole
    && hasAnyRole(userStore.userInfo.roleLabels, [AUTH_ROLES.GOV_ADMIN])
    && hasResourcePath(userStore.userInfo.resourcePaths, "/web/agent/prompt/page");
});

const canAccessSystemManage = computed(() => {
  return canManageUsers.value || canAccessPermission.value || canAccessCacheControl.value || canAccessServiceControl.value || canAccessLoginMonitor.value || canAccessPromptConsole.value;
});

const canManageVectorStore = computed(() => {
  return hasResourcePath(userStore.userInfo.resourcePaths, ["/web/vector-store/stats", "/web/vector-store/documents"]);
});

const canAccessAgent = computed(() => {
  return hasResourcePath(userStore.userInfo.resourcePaths, "/web/agent/sence");
});

const canAccessMonitor = computed(() => {
  return hasResourcePath(userStore.userInfo.resourcePaths, "/web/video/list");
});

const canAccessNursingOrg = computed(() => {
  return hasResourcePath(userStore.userInfo.resourcePaths, ["/web/nursing-task/page", "/web/nursing-log/page", "/web/nursing-task-template/page"]);
});

const canAccessMyNursingTask = computed(() => {
  return hasResourcePath(userStore.userInfo.resourcePaths, "/web/nursing-task/my/page");
});

const canAccessWriteNursingLog = computed(() => {
  return hasResourcePath(userStore.userInfo.resourcePaths, ["/web/nursing-log/my/page", "/web/nursing-log/add"]);
});

const canAccessNursingPersonal = computed(() => {
  return canAccessMyNursingTask.value || canAccessWriteNursingLog.value;
});

const userAvatarSrc = computed(() => {
  return buildImageProxySrc(userStore.userInfo.avatar || "");
});

const hydrateUserProfileForNav = async () => {
  if (!userStore.userInfo.token) {
    return;
  }
  try {
    await userStore.syncCurrentUserInfo();
  } catch (e) {
    console.warn("菜单栏权限刷新失败", e);
  }
  // 仅在菜单头像缺失时兜底拉取，避免多余请求
  if ((userStore.userInfo.avatar || "").trim()) {
    return;
  }
  try {
    const res: any = await getCurrentUserAPI();
    if (res?.code === 200 && res?.data) {
      userStore.userInfo.avatar = res.data.avatar ?? userStore.userInfo.avatar;
      userStore.userInfo.username = res.data.username ?? userStore.userInfo.username;
      userStore.userInfo.email = res.data.email ?? userStore.userInfo.email;
      userStore.userInfo.phoneNumber = res.data.phoneNumber ?? userStore.userInfo.phoneNumber;
    }
  } catch (e) {
    console.warn("菜单栏头像回填失败", e);
  }
};

onMounted(() => {
  hydrateUserProfileForNav();
});

const activeMenu = computed(() => {
  const path = route.path;
  if (path.startsWith("/nursingHome")) return "/nursingHome";
  if (path === "/userManage" || path === "/permission" || path === "/CacheControl" || path === "/ServiceControl" || path === "/LoginMonitor" || path === "/promptConsole") return "/systemManage";
  if (path === "/nursingTaskDispatch" || path === "/nursingLog" || path === "/nursingTaskTemplate") return "/nursingOrg";
  if (path === "/myNursingTask" || path === "/writeNursingLog") return "/nursingPersonal";
  return path;
});

const handleMenuSelect = (index: string) => {
  if (index.startsWith("/") && index !== route.path) {
    router.push(index);
  }
};

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      "退出登录后需要重新输入账号密码才能访问系统，确定要退出吗？",
      "退出确认",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
        center: true,
        customClass: "logout-confirm-dialog",
      }
    );
    await userStore.logout();
    router.push("/login");
  } catch {
    // 用户取消退出，不做任何操作
  }
};
</script>

<style scoped>
.nav-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 16px;
  box-sizing: border-box;
  background: #ffffff;
  border-bottom: 1px solid #dcdfe6;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.nav-brand {
  display: flex;
  align-items: center;
  min-width: 300px;
  flex-shrink: 0;
}

.brand-logo {
  width: 40px;
  height: 40px;
  margin-right: 12px;
}

.brand-title {
  font-size: 16px;
  line-height: 1.2;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
}

.nav-menu {
  flex: 1;
  min-width: 0;
  --el-menu-horizontal-height: 60px;
  border-bottom: none;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
}

.nav-menu::-webkit-scrollbar {
  display: none;
}

.nav-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  min-width: 190px;
  flex-shrink: 0;
}

.icon-button {
  color: #606266;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #606266;
  outline: none;
  height: 36px;
  padding: 0 10px 0 4px;
  border: none;
  border-radius: 4px;
  background: #ffffff;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.user-entry:hover {
  background: #f9fbff;
}

.user-entry:focus,
.user-entry:focus-visible {
  outline: none;
  box-shadow: none;
}

.user-name {
  font-size: 14px;
  color: #606266;
}

.user-arrow {
  font-size: 12px;
  color: #909399;
}

@media (max-width: 1200px) {
  .nav-header {
    padding: 0 14px;
    gap: 12px;
  }

  .nav-brand {
    min-width: 220px;
  }

  .brand-title {
    font-size: 15px;
  }

  .nav-actions {
    min-width: auto;
    gap: 8px;
  }
}

@media (max-width: 980px) {
  .nav-header {
    gap: 10px;
    padding: 0 10px;
  }

  .nav-brand {
    min-width: auto;
    max-width: 200px;
  }

  .brand-logo {
    width: 34px;
    height: 34px;
    margin-right: 8px;
  }

  .brand-title {
    font-size: 14px;
  }

  .nav-actions {
    gap: 6px;
  }

  .user-name {
    max-width: 64px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 768px) {
  .nav-header {
    padding: 0 8px;
  }

  .brand-title {
    display: none;
  }

  .nav-brand {
    flex-shrink: 0;
  }

  .nav-actions {
    min-width: auto;
  }

  .user-entry {
    padding-right: 4px;
  }

  .user-name {
    display: none;
  }
}
</style>

<style>
.logout-confirm-dialog {
  width: min(520px, calc(100vw - 32px));
  border-radius: 12px;
  border: 1px solid #d8e2f2;
  box-shadow: 0 20px 44px rgba(21, 52, 94, 0.22);
  padding: 8px 10px 12px;
}

.logout-confirm-dialog .el-message-box__header {
  padding: 6px 10px 8px;
}

.logout-confirm-dialog .el-message-box__title {
  font-size: 22px;
  font-weight: 700;
  color: #183761;
  letter-spacing: 0.01em;
}

.logout-confirm-dialog .el-message-box__headerbtn {
  top: 14px;
  right: 14px;
}

.logout-confirm-dialog .el-message-box__close {
  color: #8aa3c8;
  font-size: 18px;
}

.logout-confirm-dialog .el-message-box__content {
  padding: 6px 12px 16px;
}

.logout-confirm-dialog .el-message-box__message {
  margin: 0;
  text-align: left;
}

.logout-confirm-dialog .el-message-box__message p {
  margin: 0;
  color: #4f6485;
  font-size: 15px;
  line-height: 1.65;
  letter-spacing: 0;
  word-break: break-word;
  white-space: normal;
}

.logout-confirm-dialog .el-message-box__status {
  font-size: 22px !important;
  color: #f5a623 !important;
  margin-top: 5px;
}

.logout-confirm-dialog .el-message-box__btns {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 6px 10px 8px;
}

.logout-confirm-dialog .el-message-box__btns .el-button {
  min-width: 86px;
  height: 36px;
  border-radius: 8px;
  font-weight: 600;
  margin-left: 0;
}

.logout-confirm-dialog .el-message-box__btns .el-button--default {
  border-color: #c8d7ed;
  color: #4f6485;
  background: #f7faff;
}

.logout-confirm-dialog .el-message-box__btns .el-button--default:hover {
  border-color: #a8c0e2;
  color: #355783;
  background: #eef4ff;
}

.logout-confirm-dialog .el-message-box__btns .el-button--primary {
  border-color: #3f79d0;
  background: linear-gradient(180deg, #4f88de 0%, #3f79d0 100%);
}

.logout-confirm-dialog .el-message-box__btns .el-button--primary:hover {
  border-color: #3469b8;
  background: linear-gradient(180deg, #5b93e6 0%, #467fd4 100%);
}

.nav-submenu-popper .el-menu--popup {
  min-width: 0 !important;
  width: max-content;
}

.nav-submenu-popper .el-menu-item {
  height: 40px;
  line-height: 40px;
  padding: 0 12px;
}
</style>
