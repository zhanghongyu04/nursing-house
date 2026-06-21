<template>
  <div class="login-monitor-page">
    <section class="summary-row">
      <div class="metric-card">
        <div class="metric-label">在线用户</div>
        <div class="metric-value">{{ onlineUsers.length }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">登录成功记录</div>
        <div class="metric-value success">{{ successCount }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">登录失败记录</div>
        <div class="metric-value danger">{{ failureCount }}</div>
      </div>
    </section>

    <section v-if="canViewOnlineUsers" class="monitor-box">
      <div class="box-header">
        <span>用户在线状态</span>
        <el-button size="small" @click="fetchOnlineUsers">刷新</el-button>
      </div>
      <el-table :data="onlineUsers" border stripe class="monitor-table" empty-text="暂无在线用户">
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="role in row.roleLabels || []" :key="role" size="small" class="role-tag">
              {{ role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sanaId" label="机构ID" width="100" />
        <el-table-column prop="lastLoginTime" label="最近登录时间" min-width="180" />
        <el-table-column label="剩余有效期" width="130">
          <template #default="{ row }">
            {{ formatExpire(row.expireSeconds) }}
          </template>
        </el-table-column>
        <el-table-column prop="token" label="Token" min-width="150" />
        <el-table-column label="状态" width="100">
          <template #default>
            <el-tag type="success">在线</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canKickOutUser"
              type="danger"
              link
              :disabled="row.username === currentUsername"
              @click="handleKickOut(row)"
            >
              踢下线
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="canViewLoginLogs" class="monitor-box">
      <div class="box-header">
        <span>登录记录</span>
      </div>
      <div class="filter-bar">
        <el-input
          v-model="query.username"
          clearable
          placeholder="按用户名搜索"
          class="filter-input"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.successFlag" clearable placeholder="登录结果" class="filter-select">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="loginLogs" border stripe class="monitor-table" empty-text="暂无登录记录">
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="loginIp" label="登录IP" min-width="140" />
        <el-table-column prop="loginLocation" label="登录地点" min-width="140" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.successFlag === 1 ? 'success' : 'danger'">
              {{ row.successFlag === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column prop="loginTime" label="登录时间" min-width="180" />
        <el-table-column prop="userAgent" label="客户端" min-width="260" show-overflow-tooltip />
      </el-table>
      <div class="pagination-row">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchLoginLogs"
          @current-change="fetchLoginLogs"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getLoginLogsAPI, getOnlineUsersAPI, kickOutUserAPI } from "@/api/monitor";
import { useUserStore } from "@/stores/userStore";
import { hasResourcePath } from "@/constants/authRoles";

interface LoginLog {
  username: string;
  loginIp: string;
  loginLocation: string;
  userAgent: string;
  successFlag: number;
  message: string;
  loginTime: string;
}

interface OnlineUser {
  username: string;
  sanaId?: number;
  roleLabels?: string[];
  token: string;
  expireSeconds: number;
  lastLoginTime?: string;
}

const onlineUsers = ref<OnlineUser[]>([]);
const loginLogs = ref<LoginLog[]>([]);
const total = ref(0);
const successCount = ref(0);
const failureCount = ref(0);
const userStore = useUserStore();
const currentUsername = computed(() => userStore.userInfo.username || "");
const canViewLoginLogs = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, "/web/monitor/loginLogs"));
const canViewOnlineUsers = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, "/web/monitor/onlineUsers"));
const canKickOutUser = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, "/web/monitor/kickOut"));

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: "",
  successFlag: "" as number | "",
});

const currentQuery = computed(() => ({
  pageNum: query.pageNum,
  pageSize: query.pageSize,
  username: query.username || undefined,
  successFlag: query.successFlag === "" ? undefined : query.successFlag,
}));

const fetchOnlineUsers = async () => {
  if (!canViewOnlineUsers.value) {
    onlineUsers.value = [];
    return;
  }
  const res: any = await getOnlineUsersAPI();
  onlineUsers.value = Array.isArray(res?.data) ? res.data : [];
};

const fetchLoginLogs = async () => {
  if (!canViewLoginLogs.value) {
    loginLogs.value = [];
    total.value = 0;
    return;
  }
  const res: any = await getLoginLogsAPI(currentQuery.value);
  const data = res?.data || {};
  loginLogs.value = data.records || [];
  total.value = Number(data.total || 0);
};

const fetchSummary = async () => {
  if (!canViewLoginLogs.value) {
    successCount.value = 0;
    failureCount.value = 0;
    return;
  }
  const [successRes, failureRes]: any[] = await Promise.all([
    getLoginLogsAPI({ pageNum: 1, pageSize: 1, successFlag: 1 }),
    getLoginLogsAPI({ pageNum: 1, pageSize: 1, successFlag: 0 }),
  ]);
  successCount.value = Number(successRes?.data?.total || 0);
  failureCount.value = Number(failureRes?.data?.total || 0);
};

const handleSearch = () => {
  query.pageNum = 1;
  fetchLoginLogs();
};

const handleReset = () => {
  query.pageNum = 1;
  query.pageSize = 10;
  query.username = "";
  query.successFlag = "";
  fetchLoginLogs();
};

const handleKickOut = async (row: OnlineUser) => {
  if (!canKickOutUser.value) {
    ElMessage.warning("当前账号无踢下线权限");
    return;
  }
  await ElMessageBox.confirm(
    `确定要将用户“${row.username}”踢下线吗？`,
    "踢下线确认",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }
  );
  await kickOutUserAPI(row.username);
  ElMessage.success("用户已踢下线");
  await fetchOnlineUsers();
};

const formatExpire = (seconds?: number) => {
  const value = Number(seconds);
  if (!Number.isFinite(value) || value < 0) {
    return "未知";
  }
  const minutes = Math.floor(value / 60);
  if (minutes < 60) {
    return `${minutes}分钟`;
  }
  return `${Math.floor(minutes / 60)}小时${minutes % 60}分钟`;
};

onMounted(() => {
  fetchOnlineUsers();
  fetchLoginLogs();
  fetchSummary();
});
</script>

<style scoped>
.login-monitor-page {
  min-height: calc(100vh - 60px);
  padding: 20px 120px 32px;
  background: #ffffff;
  box-sizing: border-box;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.metric-card,
.monitor-box {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #ffffff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
}

.metric-card {
  padding: 18px 20px;
}

.metric-label {
  color: #909399;
  font-size: 14px;
}

.metric-value {
  margin-top: 8px;
  color: #303133;
  font-size: 30px;
  font-weight: 700;
}

.metric-value.success {
  color: #67c23a;
}

.metric-value.danger {
  color: #f56c6c;
}

.monitor-box {
  margin-bottom: 18px;
}

.box-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  color: #707070;
  font-size: 16px;
  font-weight: 500;
}

.filter-bar {
  display: flex;
  gap: 10px;
  padding: 16px 20px 0;
}

.filter-input {
  width: 220px;
}

.filter-select {
  width: 150px;
}

.monitor-table {
  width: calc(100% - 40px);
  margin: 16px 20px 20px;
}

.role-tag {
  margin-right: 6px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding: 0 20px 20px;
}

@media (max-width: 1100px) {
  .login-monitor-page {
    padding: 16px;
  }

  .summary-row {
    grid-template-columns: 1fr;
  }
}
</style>
