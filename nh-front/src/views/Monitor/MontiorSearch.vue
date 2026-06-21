<template>
  <div class="monitor-root">
    <!-- 顶栏 -->
    <header class="top-bar">
      <div class="top-left">
        <h1 class="page-title">视频监控</h1>
        <el-select
          v-model="selectedSanaId"
          placeholder="选择机构"
          clearable
          class="sana-select"
          @change="onSanaChange"
        >
          <el-option
            v-for="s in sanatoriums"
            :key="s.id"
            :label="s.name"
            :value="s.id"
          />
        </el-select>
      </div>
      <div class="top-right">
        <button v-if="canAddCamera" class="bar-btn" @click="openAddCamera">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/></svg>
          添加设备
        </button>
        <button class="bar-btn" :disabled="isRefreshing" @click="handleManualRefresh">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182"/></svg>
          {{ isRefreshing ? '刷新中...' : '刷新' }}
        </button>
        <span class="last-refresh">{{ autoRefreshLabel }}</span>
      </div>
    </header>

    <!-- 主体三栏 -->
    <div :key="monitorRefreshKey" class="monitor-body">
      <!-- ===== 左栏：设备列表 ===== -->
      <aside class="sidebar-left">
        <div class="sidebar-header">
          <span class="sidebar-title">设备列表</span>
          <span class="device-count">{{ cameras.length }} 台</span>
        </div>

        <!-- 搜索 -->
        <div class="search-box">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"/></svg>
          <input v-model="deviceSearch" class="search-input" placeholder="搜索设备名称 / 位置" />
        </div>

        <!-- 状态筛选 -->
        <div class="filter-row">
          <button
            v-for="f in statusFilters"
            :key="f.key"
            class="filter-chip"
            :class="{ active: activeFilter === f.key }"
            @click="activeFilter = f.key"
          >{{ f.label }} ({{ f.count }})</button>
        </div>

        <!-- 设备列表 -->
        <div class="device-list">
          <div
            v-for="cam in filteredCameras"
            :key="cam.id"
            class="device-item"
            :class="{ selected: activeCamera?.id === cam.id }"
            @click="selectCamera(cam)"
          >
            <span class="dev-status" :class="getCameraRuntimeStatus(cam)"></span>
            <div class="dev-info">
              <span class="dev-name">{{ cam.cameraName }}</span>
              <span class="dev-loc">{{ cam.cameraLocation }}</span>
            </div>
            <div class="dev-actions">
              <button v-if="canUpdateCamera" class="icon-btn" title="编辑" @click.stop="openEditCamera(cam)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931z"/></svg>
              </button>
              <button v-if="canDeleteCamera" class="icon-btn danger" title="删除" @click.stop="confirmDeleteCamera(cam)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0"/></svg>
              </button>
            </div>
          </div>
          <div v-if="filteredCameras.length === 0" class="empty-state">
            <span>暂无设备</span>
          </div>
        </div>
      </aside>

      <!-- ===== 中栏：播放器 + 告警 ===== -->
      <main class="center-col">
        <!-- 视频播放器 -->
        <div class="player-section">
          <div v-if="activeCamera?.id" class="player-wrapper">
            <EzvizLivePlayer
              :camera="activeCamera"
              @loaded="onPlayerLoaded"
              @status-change="onPlayerStatusChange"
            />
          </div>
          <div v-else class="no-player">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="no-player-icon">
              <path d="m15.75 10.5 4.72-4.72a.75.75 0 0 1 1.28.53v11.38a.75.75 0 0 1-1.28.53l-4.72-4.72M4.5 18.75h9a2.25 2.25 0 0 0 2.25-2.25v-9a2.25 2.25 0 0 0-2.25-2.25h-9A2.25 2.25 0 0 0 2.25 7.5v9a2.25 2.25 0 0 0 2.25 2.25Z"/>
            </svg>
            <span>选择左侧设备开始预览</span>
          </div>
        </div>

        <!-- 状态记录 -->
        <div class="alert-section">
          <div class="alert-header">
            <span class="alert-title">状态记录</span>
          </div>
          <div class="alert-list">
            <div
              v-for="a in alerts"
              :key="a.id"
              class="alert-item"
              :class="{ handled: a.status === 1 }"
            >
              <span class="alert-status-dot" :class="isOnlineContent(a.content) ? 'done' : 'pending'"></span>
              <div class="alert-body">
                <div class="alert-top-row">
                  <span class="alert-cam">{{ a.cameraName || '--' }}</span>
                  <span class="alert-time">{{ formatDateTime(a.alertTime) }}</span>
                </div>
                <p class="alert-content">{{ a.content }}</p>
                <p v-if="a.handleRemark" class="alert-remark">处理备注：{{ a.handleRemark }}</p>
                <div class="alert-meta-row">
                  <span class="alert-state" :class="a.status === 1 ? 'done' : 'pending'">
                    {{ a.status === 1 ? '已处理' : '未处理' }}
                  </span>
                  <button
                    v-if="a.status !== 1 && canHandleAlert"
                    class="handle-btn"
                    type="button"
                    @click="openHandleAlert(a)"
                  >
                    处理
                  </button>
                </div>
              </div>
            </div>
            <div v-if="alerts.length === 0" class="empty-state small">暂无状态记录</div>
          </div>
        </div>
      </main>

      <!-- ===== 右栏：设备信息 + 云台 ===== -->
      <aside class="sidebar-right">
        <!-- 设备详情 -->
        <div class="info-card">
          <div class="card-header">设备信息</div>
          <template v-if="activeCamera">
            <div class="info-rows">
              <div class="info-row"><span class="label">设备名称</span><span class="val">{{ activeCamera.cameraName }}</span></div>
              <div class="info-row"><span class="label">安装位置</span><span class="val">{{ activeCamera.cameraLocation }}</span></div>
              <div class="info-row"><span class="label">序列号</span><span class="val mono">{{ activeCamera.deviceSerial || '--' }}</span></div>
              <div class="info-row"><span class="label">通道号</span><span class="val">{{ activeCamera.channelNo ?? '--' }}</span></div>
              <div class="info-row"><span class="label">画质</span><span class="val">{{ formatStreamQuality(activeCamera.streamQuality) }}</span></div>
              <div class="info-row"><span class="label">在线状态</span><span class="val"><span class="dev-status sm" :class="getCameraRuntimeStatus(activeCamera)"></span>{{ statusLabel(activeCamera) }}</span></div>
              <div class="info-row"><span class="label">所属机构</span><span class="val">{{ activeCamera.sanaName || '--' }}</span></div>
              <div class="info-row"><span class="label">更新时间</span><span class="val">{{ formatLatestTime(activeCamera.updateTime) }}</span></div>
            </div>
          </template>
          <div v-else class="empty-state small">选择设备查看详情</div>
        </div>

        <!-- 云台控制 -->
        <MonitorPtzPanel :camera="activeCamera" />
      </aside>
    </div>

    <!-- ===== 添加/编辑设备弹窗 ===== -->
    <el-dialog
      v-model="showAddDialog"
      :title="editingCamera ? '编辑设备' : '添加设备'"
      width="480px"
      :close-on-click-modal="false"
      class="monitor-dialog"
    >
      <el-form :model="cameraForm" label-width="80px" label-position="left">
        <el-form-item label="所属机构" required>
          <el-select v-model="cameraForm.sanaId" placeholder="选择机构" class="full">
            <el-option v-for="s in sanatoriums" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备名称" required>
          <el-input v-model="cameraForm.cameraName" placeholder="如：大厅摄像头" />
        </el-form-item>
        <el-form-item label="安装位置" required>
          <el-input v-model="cameraForm.cameraLocation" placeholder="如：一楼大厅" />
        </el-form-item>
        <el-form-item label="序列号" required>
          <el-input v-model="cameraForm.deviceSerial" placeholder="萤石设备序列号" />
        </el-form-item>
        <el-form-item label="通道号">
          <el-input-number v-model="cameraForm.channelNo" :min="1" :max="99" />
        </el-form-item>
        <el-form-item label="验证码">
          <el-input v-model="cameraForm.validateCode" placeholder="设备验证码（可选）" />
        </el-form-item>
        <el-form-item label="画质">
          <el-select v-model="cameraForm.streamQuality">
            <el-option label="高清" value="HD" />
            <el-option label="标清" value="SD" />
            <el-option label="流畅" value="LD" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCameraForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EzvizLivePlayer from '@/components/video/EzvizLivePlayer.vue'
import MonitorPtzPanel from './MonitorPtzPanel.vue'
import { useUserStore } from '@/stores/userStore'
import { hasResourcePath } from '@/constants/authRoles'
import {
  getCameraListAPI,
  addCameraAPI,
  deleteCameraAPI,
  updateCameraAPI,
  getVideoAlertsAPI,
  handleVideoAlertAPI,
  type CameraItem,
  type VideoAlertItem
} from '@/api/video'
import {
  normalizeCamera,
  getCameraRuntimeStatus,
  formatStreamQuality,
  formatDateTime,
  formatLatestTime
} from './monitor.shared'
import { pageSanatoriumAPI } from '@/api/sanatorium'

// ──────────── 机构列表 ────────────
interface Sanatorium {
  id: number
  name: string
}

const userStore = useUserStore()
const sanatoriums = ref<Sanatorium[]>([])
const selectedSanaId = ref<number | undefined>(undefined)
const hasGlobalSanaScope = computed(() => {
  return !userStore.userInfo.sanaId && (userStore.userInfo.sanaScopeIds || []).length === 0
})
const canAddCamera = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/video/add'))
const canUpdateCamera = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/video/update'))
const canDeleteCamera = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/video/delete'))
const canHandleAlert = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/video/handleAlert'))

// ──────────── 设备列表 ────────────
const cameras = ref<CameraItem[]>([])
const activeCamera = ref<CameraItem | null>(null)
const deviceSearch = ref('')
const activeFilter = ref<'all' | 'online' | 'offline' | 'fault'>('all')
const activePlayerConnected = ref(false)

const statusFilters = computed(() => {
  const all = cameras.value.length
  const online = cameras.value.filter(c => getCameraRuntimeStatus(c) === 'online').length
  const offline = cameras.value.filter(c => getCameraRuntimeStatus(c) === 'offline').length
  const fault = cameras.value.filter(c => getCameraRuntimeStatus(c) === 'fault').length
  return [
    { key: 'all' as const, label: '全部', count: all },
    { key: 'online' as const, label: '在线', count: online },
    { key: 'offline' as const, label: '离线', count: offline },
    { key: 'fault' as const, label: '故障', count: fault }
  ]
})

const filteredCameras = computed(() => {
  let list = cameras.value
  if (activeFilter.value !== 'all') {
    list = list.filter(c => getCameraRuntimeStatus(c) === activeFilter.value)
  }
  if (deviceSearch.value.trim()) {
    const q = deviceSearch.value.trim().toLowerCase()
    list = list.filter(c =>
      c.cameraName?.toLowerCase().includes(q) ||
      c.cameraLocation?.toLowerCase().includes(q)
    )
  }
  return list
})

const statusLabel = (cam: CameraItem) => {
  const s = getCameraRuntimeStatus(cam)
  if (s === 'online') return '在线'
  if (s === 'fault') return '故障'
  return '离线'
}

// ──────────── 告警 ────────────
const alerts = ref<VideoAlertItem[]>([])

const isOnlineContent = (content?: string) => content?.includes('上线') ?? false

const openHandleAlert = async (alert: VideoAlertItem) => {
  if (!canHandleAlert.value) {
    ElMessage.warning('当前账号无处理状态记录权限')
    return
  }
  if (!alert.id) {
    ElMessage.warning('状态记录缺少编号')
    return
  }
  if (alert.status === 1) {
    ElMessage.info('该记录已处理')
    return
  }

  try {
    const { value } = await ElMessageBox.prompt('可填写处理备注，留空则直接标记为已处理。', '处理状态记录', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: alert.handleRemark || '',
      inputPlaceholder: '输入处理备注'
    })
    const res = asResult<boolean>(await handleVideoAlertAPI({
      alertId: alert.id,
      handleRemark: value?.trim() || undefined
    }))
    if (res.code === 200) {
      ElMessage.success('处理成功')
      await loadAlerts()
      return
    }
    ElMessage.error(res.message || '处理失败')
  } catch (e) {
    if (e === 'cancel' || e === 'close') {
      return
    }
    console.error(e)
    ElMessage.error('处理失败')
  }
}

// ──────────── 弹窗 ────────────
const showAddDialog = ref(false)
const submitting = ref(false)
const editingCamera = ref<CameraItem | null>(null)

const cameraForm = ref<{
  sanaId: number | undefined
  cameraName: string
  cameraLocation: string
  deviceSerial: string
  channelNo: number
  validateCode: string
  streamQuality: string
  id?: number
}>({
  sanaId: undefined,
  cameraName: '',
  cameraLocation: '',
  deviceSerial: '',
  channelNo: 1,
  validateCode: '',
  streamQuality: 'HD'
})

// ──────────── 自动刷新 ────────────
let refreshTimer: ReturnType<typeof setInterval> | null = null
let lastRefreshTime = ref<Date>(new Date())
const isRefreshing = ref(false)
const monitorRefreshKey = ref(0)

const autoRefreshLabel = computed(() => {
  const diff = Date.now() - lastRefreshTime.value.getTime()
  if (diff < 60000) return '刚刚刷新'
  return `${Math.floor(diff / 60000)} 分钟前刷新`
})

// ──────────── API 结果类型 ────────────
interface ApiResult<T> {
  code: number
  message?: string
  data?: T
}

const asResult = <T>(res: unknown): ApiResult<T> => res as ApiResult<T>

const ensureDefaultSelectedSanaId = () => {
  if (selectedSanaId.value && sanatoriums.value.some(item => item.id === selectedSanaId.value)) {
    return
  }

  const currentSanaId = userStore.userInfo.sanaId
  const preferredSanatorium = sanatoriums.value.find(item => item.id === currentSanaId) || sanatoriums.value[0]
  selectedSanaId.value = preferredSanatorium?.id
}

const shouldSkipScopedQuery = () => {
  return !hasGlobalSanaScope.value && !selectedSanaId.value && sanatoriums.value.length > 1
}

// ──────────── 数据加载 ────────────
const loadSanatoriums = async () => {
  try {
    // 从养老院分页接口获取全量机构（取大页）
    const res = asResult<{ records: any[]; total: number }>(await pageSanatoriumAPI({ page: 1, pageSize: 999 }))
    if (res.code === 200 && res.data?.records) {
      sanatoriums.value = res.data.records.map((r: any) => ({
        id: r.id || r.sanaId,
        name: r.sanaName || r.name || ''
      }))
      ensureDefaultSelectedSanaId()
    }
  } catch (e) {
    console.error(e)
  }
}

const loadCameras = async () => {
  try {
    if (shouldSkipScopedQuery()) {
      cameras.value = []
      activeCamera.value = null
      return true
    }
    const selector: Record<string, any> = {}
    if (selectedSanaId.value) {
      selector.sanaId = selectedSanaId.value
    }
    const res = asResult<CameraItem[]>(await getCameraListAPI(selector))
    if (res.code === 200 && res.data) {
      cameras.value = res.data.map(normalizeCamera)
      // 保持选中状态
      if (activeCamera.value?.id) {
        const found = cameras.value.find(c => c.id === activeCamera.value!.id)
        if (!found) {
          activeCamera.value = cameras.value[0] || null
        }
      } else if (cameras.value.length > 0) {
        activeCamera.value = cameras.value[0]
      }
    }
    return true
  } catch (e) {
    console.error(e)
    return false
  }
}

const loadAlerts = async () => {
  try {
    if (shouldSkipScopedQuery()) {
      alerts.value = []
      return true
    }
    const selector: Record<string, any> = {}
    if (selectedSanaId.value) {
      selector.sanaId = selectedSanaId.value
    }
    const res = asResult<VideoAlertItem[]>(await getVideoAlertsAPI(selector))
    if (res.code === 200 && res.data) {
      alerts.value = res.data
    }
    return true
  } catch (e) {
    console.error(e)
    return false
  }
}

const refreshAll = async (opts?: { silent?: boolean }) => {
  if (isRefreshing.value) {
    return
  }

  isRefreshing.value = true
  try {
    const [cameraOk, alertOk] = await Promise.all([loadCameras(), loadAlerts()])
    lastRefreshTime.value = new Date()

    if (!opts?.silent) {
      if (cameraOk && alertOk) {
        ElMessage.success('刷新成功')
      } else {
        ElMessage.warning('刷新部分成功，请检查网络或后端服务')
      }
    }

    // 手动刷新触发容器重建，确保播放器等子组件也会重刷
    if (opts?.silent === false) {
      monitorRefreshKey.value += 1
    }
  } finally {
    isRefreshing.value = false
  }
}

const handleManualRefresh = async () => {
  await refreshAll({ silent: false })
}

const onSanaChange = () => {
  activeCamera.value = null
  refreshAll({ silent: true })
}

const selectCamera = (cam: CameraItem) => {
  activeCamera.value = cam
}

// ──────────── 播放器事件 ────────────
const onPlayerLoaded = (_config: any) => {
  // playConfig loaded
}

const onPlayerStatusChange = (connected: boolean) => {
  activePlayerConnected.value = connected
}

// ──────────── 设备 CRUD ────────────
const openAddCamera = () => {
  if (!canAddCamera.value) {
    ElMessage.warning('当前账号无添加设备权限')
    return
  }
  showAddDialog.value = true
}

const openEditCamera = (cam: CameraItem) => {
  if (!canUpdateCamera.value) {
    ElMessage.warning('当前账号无编辑设备权限')
    return
  }
  editingCamera.value = cam
  cameraForm.value = {
    sanaId: cam.sanaId,
    cameraName: cam.cameraName,
    cameraLocation: cam.cameraLocation,
    deviceSerial: cam.deviceSerial || '',
    channelNo: cam.channelNo || 1,
    validateCode: cam.validateCode || '',
    streamQuality: cam.streamQuality || 'HD',
    id: cam.id
  }
  showAddDialog.value = true
}

const confirmDeleteCamera = async (cam: CameraItem) => {
  if (!canDeleteCamera.value) {
    ElMessage.warning('当前账号无删除设备权限')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除设备「${cam.cameraName}」？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = asResult<any>(await deleteCameraAPI(cam.id!))
    if (res.code === 200) {
      ElMessage.success('删除成功')
      if (activeCamera.value?.id === cam.id) {
        activeCamera.value = null
      }
      await loadCameras()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
    // 用户取消
  }
}

const submitCameraForm = async () => {
  if (editingCamera.value && !canUpdateCamera.value) {
    ElMessage.warning('当前账号无编辑设备权限')
    return
  }
  if (!editingCamera.value && !canAddCamera.value) {
    ElMessage.warning('当前账号无添加设备权限')
    return
  }
  const form = cameraForm.value
  if (!form.sanaId || !form.cameraName || !form.cameraLocation || !form.deviceSerial) {
    ElMessage.warning('请填写必填项')
    return
  }

  submitting.value = true
  try {
    if (editingCamera.value) {
      const res = asResult<any>(await updateCameraAPI({
        id: editingCamera.value.id,
        ...form
      }))
      if (res.code === 200) {
        ElMessage.success('更新成功')
      } else {
        ElMessage.error(res.message || '更新失败')
      }
    } else {
      const res = asResult<any>(await addCameraAPI(form))
      if (res.code === 200) {
        ElMessage.success('添加成功')
      } else {
        ElMessage.error(res.message || '添加失败')
      }
    }
    showAddDialog.value = false
    editingCamera.value = null
    resetForm()
    await loadCameras()
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err.response?.data?.message || err.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  cameraForm.value = {
    sanaId: selectedSanaId.value,
    cameraName: '',
    cameraLocation: '',
    deviceSerial: '',
    channelNo: 1,
    validateCode: '',
    streamQuality: 'HD'
  }
}

// ──────────── 生命周期 ────────────
onMounted(async () => {
  // 监听添加弹窗关闭事件重置表单
  await loadSanatoriums()
  await refreshAll({ silent: true })
  // 自动刷新 15s
  refreshTimer = setInterval(() => {
    refreshAll({ silent: true })
  }, 15000)
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})

// 弹窗打开时重置编辑状态
import { watch } from 'vue'
watch(showAddDialog, (v) => {
  if (v && !editingCamera.value) {
    resetForm()
  }
  if (!v) {
    editingCamera.value = null
  }
})
</script>

<style scoped>
/* ====== 全局 ====== */
.monitor-root {
  --bg-primary: #f3f6fa;
  --bg-secondary: #ffffff;
  --bg-tertiary: #f8fafc;
  --border: #d7e0ea;
  --text-primary: #1a2f4d;
  --text-secondary: #5d728f;
  --text-muted: #4f6687;
  --accent: #3b6fb1;
  --accent-hover: #2f5e9e;
  --danger: #f56c6c;
  --success: #67c23a;
  --warning: #e6a23c;

  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif;
  overflow: hidden;
}

/* ====== 顶栏 ====== */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 52px;
  min-height: 52px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);
}

.top-left,
.top-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.sana-select {
  width: 180px;
}

.sana-select :deep(.el-input__wrapper) {
  background: var(--bg-tertiary);
  border-color: var(--border);
  box-shadow: none;
}

.bar-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.bar-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.bar-btn svg {
  width: 14px;
  height: 14px;
}

.bar-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.bar-btn.sm {
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
}

.last-refresh {
  color: var(--text-muted);
  font-size: 11px;
}

/* ====== 三栏主体 ====== */
.monitor-body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ====== 左栏 ====== */
.sidebar-left {
  width: 280px;
  min-width: 280px;
  display: flex;
  flex-direction: column;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border);
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 8px;
}

.sidebar-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.device-count {
  font-size: 11px;
  color: var(--text-muted);
}

.search-box {
  position: relative;
  padding: 0 12px 8px;
}

.search-icon {
  position: absolute;
  left: 22px;
  top: 50%;
  transform: translateY(calc(-50% - 4px));
  width: 14px;
  height: 14px;
  color: var(--text-muted);
}

.search-input {
  width: 100%;
  height: 32px;
  padding: 0 12px 0 34px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s;
}

.search-input::placeholder {
  color: var(--text-muted);
}

.search-input:focus {
  border-color: var(--accent);
}

.filter-row {
  display: flex;
  gap: 4px;
  padding: 0 12px 10px;
}

.filter-chip {
  padding: 3px 8px;
  border-radius: 4px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 11px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.filter-chip:hover {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.filter-chip.active {
  background: var(--accent);
  color: #fff;
}

.device-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}

.device-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.12s;
}

.device-item:hover {
  background: var(--bg-tertiary);
}

.device-item.selected {
  background: rgba(59, 111, 177, 0.1);
}

.dev-status {
  width: 8px;
  height: 8px;
  min-width: 8px;
  border-radius: 50%;
}

.dev-status.online { background: var(--success); box-shadow: 0 0 4px rgba(63, 185, 80, 0.5); }
.dev-status.offline { background: var(--text-muted); }
.dev-status.fault { background: var(--danger); }

.dev-status.sm {
  width: 6px;
  height: 6px;
  min-width: 6px;
  margin-right: 4px;
  display: inline-block;
  vertical-align: middle;
}

.dev-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dev-name {
  font-size: 13px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dev-loc {
  font-size: 11px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dev-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
}

.device-item:hover .dev-actions {
  opacity: 1;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
}

.icon-btn svg {
  width: 14px;
  height: 14px;
}

.icon-btn:hover {
  background: var(--bg-primary);
  color: var(--text-secondary);
}

.icon-btn.danger:hover {
  color: var(--danger);
}

/* ====== 中栏 ====== */
.center-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.player-section {
  flex: 1;
  min-height: 0;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.player-wrapper {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.player-wrapper :deep(.ez-player-wrap) {
  flex: 1;
  min-height: 0;
}

.no-player {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-muted);
  font-size: 14px;
}

.no-player-icon {
  width: 48px;
  height: 48px;
  color: var(--border);
}

/* 告警 */
.alert-section {
  height: 220px;
  min-height: 180px;
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--border);
}

.alert-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: var(--bg-secondary);
}

.alert-title {
  font-size: 13px;
  font-weight: 600;
}

.alert-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.alert-filter {
  width: 100px;
}

.alert-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 8px;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 6px;
  border-bottom: 1px solid var(--border);
}

.alert-item:last-child {
  border-bottom: none;
}

.alert-item.handled {
  opacity: 0.6;
}

.alert-status-dot {
  width: 7px;
  height: 7px;
  min-width: 7px;
  margin-top: 5px;
  border-radius: 50%;
}

.alert-status-dot.pending { background: var(--warning); }
.alert-status-dot.done { background: var(--text-muted); }

.alert-body {
  flex: 1;
  min-width: 0;
}

.alert-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.alert-cam {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
}

.alert-time {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
}

.alert-content {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.alert-remark {
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-muted);
}

.alert-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 6px;
}

.alert-state {
  font-size: 11px;
  line-height: 1;
}

.alert-state.pending {
  color: var(--warning);
}

.alert-state.done {
  color: var(--text-muted);
}

.handle-btn {
  flex-shrink: 0;
  height: 26px;
  padding: 0 12px;
  border-radius: 4px;
  border: 1px solid var(--accent);
  background: transparent;
  color: var(--accent);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.handle-btn:hover {
  background: var(--accent);
  color: #fff;
}

/* ====== 右栏 ====== */
.sidebar-right {
  width: 280px;
  min-width: 280px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  background: var(--bg-secondary);
  border-left: 1px solid var(--border);
  overflow-y: auto;
}

.info-card {
  background: var(--bg-primary);
  border-radius: 8px;
  border: 1px solid var(--border);
}

.card-header {
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border);
}

.info-rows {
  padding: 8px 14px 12px;
}

.info-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  padding: 5px 0;
  font-size: 12px;
}

.info-row .label {
  color: var(--text-muted);
  white-space: nowrap;
}

.info-row .val {
  color: var(--text-primary);
  text-align: right;
  word-break: break-all;
}

.info-row .val.mono {
  font-family: 'SF Mono', 'Cascadia Code', monospace;
  font-size: 11px;
}

/* ====== 空状态 ====== */
.empty-state {
  padding: 32px 0;
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
}

.empty-state.small {
  padding: 20px 0;
}

/* ====== 弹窗样式覆写 ====== */
.monitor-dialog :deep(.el-dialog) {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
}

.monitor-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid var(--border);
}

.monitor-dialog :deep(.el-dialog__title) {
  color: var(--text-primary);
}

.monitor-dialog :deep(.el-form-item__label) {
  color: var(--text-secondary);
}

.monitor-dialog :deep(.el-dialog__footer) {
  border-top: 1px solid var(--border);
}

.handle-alert-body {
  padding: 8px 0;
}

.handle-content {
  margin: 0 0 12px;
  padding: 10px;
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.full {
  width: 100%;
}

/* ====== 滚动条 ====== */
.device-list::-webkit-scrollbar,
.alert-list::-webkit-scrollbar,
.sidebar-right::-webkit-scrollbar {
  width: 5px;
}

.device-list::-webkit-scrollbar-thumb,
.alert-list::-webkit-scrollbar-thumb,
.sidebar-right::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 3px;
}

.device-list::-webkit-scrollbar-track,
.alert-list::-webkit-scrollbar-track,
.sidebar-right::-webkit-scrollbar-track {
  background: transparent;
}

/* ====== 响应式 ====== */
@media (max-width: 1024px) {
  .sidebar-left,
  .sidebar-right {
    width: 240px;
    min-width: 240px;
  }
}

@media (max-width: 768px) {
  .monitor-body {
    flex-direction: column;
  }
  .sidebar-left,
  .sidebar-right {
    width: 100%;
    min-width: 0;
    max-height: 40vh;
  }
  .sidebar-right {
    flex-direction: row;
    overflow-x: auto;
  }
}
</style>
