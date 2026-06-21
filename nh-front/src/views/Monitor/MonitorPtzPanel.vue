<template>
  <!-- 监控云台控制面板：支持八方向控制、停止和速度调节。 -->
  <div v-if="canViewPtzPanel" class="ptz-panel">
    <div class="ptz-header">
      <span class="ptz-title">云台控制</span>
      <span class="ptz-sub">{{ camera?.cameraName || '未选择设备' }}</span>
    </div>

    <template v-if="camera?.id">
      <!-- 方向键 3x3 -->
      <div class="dir-grid">
        <button
          v-for="d in directions"
          :key="d.value"
          class="dir-btn"
          :class="{ active: currentDir === d.value, center: d.value === -1 }"
          @mousedown.prevent="startControl(d.value)"
          @mouseup="stopControl"
          @mouseleave="stopControl"
          @touchstart.prevent="startControl(d.value)"
          @touchend="stopControl"
        >
          <svg v-if="d.value === 0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 19V5m0 0l-5 5m5-5l5 5"/></svg>
          <svg v-else-if="d.value === 1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14m0 0l-5-5m5 5l5-5"/></svg>
          <svg v-else-if="d.value === 2" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5m0 0l5-5m-5 5l5 5"/></svg>
          <svg v-else-if="d.value === 3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14m0 0l-5-5m5 5l-5 5"/></svg>
          <svg v-else-if="d.value === 4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 19V9.5L5.5 15M13 5v9.5L18.5 9"/></svg>
          <svg v-else-if="d.value === 5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 5v9.5L5.5 9M13 19v-9.5L18.5 15"/></svg>
          <svg v-else-if="d.value === 6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 19V9.5L18.5 15M11 5v9.5L5.5 9"/></svg>
          <svg v-else-if="d.value === 7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 5v9.5L18.5 9M11 19v-9.5L5.5 15"/></svg>
          <!-- 停止按钮 -->
          <svg v-else viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>
        </button>
      </div>

      <!-- 速度 -->
      <div class="speed-row">
        <span class="speed-label">速度</span>
        <el-slider v-model="speed" :min="1" :max="5" :step="1" :show-tooltip="false" />
        <span class="speed-val">{{ speed }}</span>
      </div>
    </template>

    <div v-else class="empty-hint">选择摄像头后可使用云台控制</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { startCameraPtzAPI, stopCameraPtzAPI, type CameraItem } from '@/api/video'
import { hasResourcePath } from '@/constants/authRoles'
import { useUserStore } from '@/stores/userStore'

const props = withDefaults(defineProps<{
  camera?: CameraItem | null
}>(), {
  camera: null
})

const currentDir = ref<number | null>(null)
const speed = ref(2)
const lastFeedback = ref<{ message: string; timestamp: number } | null>(null)
const userStore = useUserStore()
const canStartPtz = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/video/{cameraId}/ptz/start'))
const canStopPtz = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/video/{cameraId}/ptz/stop'))
const canViewPtzPanel = computed(() => canStartPtz.value || canStopPtz.value)

// 方向值与后端 PTZ 协议保持一致，-1 表示中心停止按钮。
const directions = [
  { value: 4 }, { value: 0 }, { value: 6 },
  { value: 2 }, { value: -1 }, { value: 3 },
  { value: 5 }, { value: 1 }, { value: 7 }
]

const cameraId = computed(() => props.camera?.id)

// 优先解析后端业务报错，兜底到浏览器或网络异常信息。
const resolveError = (e: unknown) => {
  const err = e as { response?: { data?: { message?: string } }; message?: string }
  return err.response?.data?.message || err.message || '操作失败'
}

// 萤石返回报文可能带有前缀，先裁剪成可读提示。
const normalizePtzMessage = (message: string) => {
  const trimmed = message.trim()
  const rawMatch = trimmed.match(/msg=(.+)$/)
  if (rawMatch?.[1]) {
    return rawMatch[1].trim()
  }
  return trimmed
}

// 将厂商原始限位提示归一化成前端统一文案。
const parsePtzFeedback = (rawMessage: string) => {
  const message = normalizePtzMessage(rawMessage)

  if (message.includes('右限位') || message.includes('右侧极限')) {
    return { type: 'warning' as const, message: '云台已到达右侧极限，无法继续向右旋转' }
  }
  if (message.includes('左限位') || message.includes('左侧极限')) {
    return { type: 'warning' as const, message: '云台已到达左侧极限，无法继续向左旋转' }
  }
  if (message.includes('上限位') || message.includes('上方极限')) {
    return { type: 'warning' as const, message: '云台已到达上方极限，无法继续向上旋转' }
  }
  if (message.includes('下限位') || message.includes('下方极限')) {
    return { type: 'warning' as const, message: '云台已到达下方极限，无法继续向下旋转' }
  }
  if (message.includes('限位') || message.includes('极限')) {
    return { type: 'warning' as const, message: '云台已到达当前方向极限' }
  }

  return { type: 'error' as const, message }
}

// 同一条限位提示短时间内只弹一次，避免长按时重复轰炸用户。
const showPtzFeedback = (rawMessage: string) => {
  const feedback = parsePtzFeedback(rawMessage)
  const now = Date.now()
  if (lastFeedback.value
    && lastFeedback.value.message === feedback.message
    && now - lastFeedback.value.timestamp < 1500) {
    return
  }

  lastFeedback.value = {
    message: feedback.message,
    timestamp: now
  }

  if (feedback.type === 'warning') {
    ElMessage.warning(feedback.message)
    return
  }
  ElMessage.error(feedback.message)
}

// 鼠标抬起、触摸结束或组件销毁时都需要下发停止指令。
const stopControl = async () => {
  if (currentDir.value === null || !cameraId.value) return
  if (!canStopPtz.value) {
    currentDir.value = null
    ElMessage.warning('当前账号无云台停止权限')
    return
  }
  currentDir.value = null
  try {
    await stopCameraPtzAPI(cameraId.value)
  } catch (e) {
    console.error(e)
  }
}

// 长按方向键时发送开始控制，请求失败则回退当前高亮态。
const startControl = async (dir: number) => {
  if (dir === -1) {
    await stopControl()
    return
  }
  if (!canStartPtz.value) {
    ElMessage.warning('当前账号无云台控制权限')
    return
  }
  if (!cameraId.value) {
    ElMessage.warning('当前设备缺少可控制标识')
    return
  }
  currentDir.value = dir
  try {
    await startCameraPtzAPI(cameraId.value, { direction: dir, speed: speed.value })
  } catch (e) {
    currentDir.value = null
    showPtzFeedback(resolveError(e))
  }
}

onBeforeUnmount(() => stopControl())
</script>

<style scoped>
.ptz-panel {
  padding: 16px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #d7e0ea;
}

.ptz-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 14px;
}

.ptz-title {
  color: #1a2f4d;
  font-size: 14px;
  font-weight: 600;
}

.ptz-sub {
  color: #5d728f;
  font-size: 12px;
}

.dir-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  width: 156px;
  margin: 0 auto 14px;
}

.dir-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border: 1px solid #d7e0ea;
  border-radius: 6px;
  background: #f8fafc;
  color: #5d728f;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.dir-btn svg {
  width: 18px;
  height: 18px;
}

.dir-btn:hover {
  background: #e7edf4;
  color: #1a2f4d;
}

.dir-btn.active {
  background: #3b6fb1;
  border-color: #3b6fb1;
  color: #fff;
}

.dir-btn.center {
  background: #f8fafc;
  border-color: #d7e0ea;
  color: #f56c6c;
}

.dir-btn.center:hover {
  background: #e7edf4;
}

.speed-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.speed-label {
  color: #5d728f;
  font-size: 12px;
  white-space: nowrap;
}

.speed-val {
  color: #3b6fb1;
  font-size: 12px;
  font-weight: 600;
  min-width: 14px;
  text-align: center;
}

.speed-row :deep(.el-slider) {
  flex: 1;
  --el-slider-main-bg-color: #3b6fb1;
  --el-slider-runway-bg-color: #d7e0ea;
  --el-slider-button-size: 14px;
}

.empty-hint {
  padding: 20px 0;
  color: #5d728f;
  font-size: 13px;
  text-align: center;
}
</style>
