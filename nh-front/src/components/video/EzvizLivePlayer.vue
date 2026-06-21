<template>
  <div class="ez-player-wrap">
    <!-- 播放区域 -->
    <div ref="stageRef" class="player-stage" :class="{ 'is-loading': isLoading, 'is-connected': isConnected }">
      <div :id="playerContainerId" ref="playerContainerRef" class="player-host"></div>

      <!-- 失败态 -->
      <div v-if="playFailed" class="state-overlay failed">
        <svg class="state-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
        </svg>
        <span>播放失败，点击重试</span>
        <button class="retry-btn" @click="reloadPlayer">重新连接</button>
      </div>
    </div>

    <!-- 底部控制栏 -->
    <div v-if="showControls" class="player-controls">
      <div class="ctrl-left">
        <span class="live-badge">LIVE</span>
        <span class="device-info">{{ playConfig?.deviceSerial || camera.deviceSerial || '--' }}</span>
        <span class="device-info">CH{{ playConfig?.channelNo || camera.channelNo || 1 }}</span>
      </div>
      <div class="ctrl-right">
        <button class="ctrl-btn" :disabled="!hasPlayer" @click="togglePlay" :title="isPlaying ? '暂停' : '播放'">
          <svg v-if="isPlaying" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="4" width="4" height="16" rx="1"/><rect x="14" y="4" width="4" height="16" rx="1"/></svg>
          <svg v-else viewBox="0 0 24 24" fill="currentColor"><path d="M8 5.14v13.72a1 1 0 001.5.86l11-6.86a1 1 0 000-1.72l-11-6.86A1 1 0 008 5.14z"/></svg>
        </button>
        <button class="ctrl-btn" :disabled="!hasPlayer" @click="toggleFullScreen" title="全屏">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M3.75 3.75v4.5m0-4.5h4.5m-4.5 0L9 9M3.75 20.25v-4.5m0 4.5h4.5m-4.5 0L9 15M20.25 3.75h-4.5m4.5 0v4.5m0-4.5L15 9m5.25 11.25h-4.5m4.5 0v-4.5m0 4.5L15 15"/></svg>
        </button>
        <span class="status-dot" :class="isConnected ? 'on' : playFailed ? 'err' : 'off'"></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { EZUIKitPlayer } from 'ezuikit-js'
import {
  getCameraPlayConfigAPI,
  type CameraItem,
  type CameraPlayConfig
} from '@/api/video'

interface ApiResult<T> {
  code: number
  message?: string
  data?: T
}

const props = withDefaults(defineProps<{
  camera: CameraItem
  showControls?: boolean
  autoplay?: boolean
}>(), {
  showControls: true,
  autoplay: true
})

const emit = defineEmits<{
  loaded: [CameraPlayConfig]
  statusChange: [boolean]
}>()

const stageRef = ref<HTMLDivElement | null>(null)
const playerContainerRef = ref<HTMLDivElement | null>(null)
const player = ref<any>(null)
const playConfig = ref<CameraPlayConfig | null>(null)
const isLoading = ref(false)
const isConnected = ref(false)
const isPlaying = ref(true)
const playFailed = ref(false)
const fallbackTimer = ref<number | null>(null)
const autoRetryCount = ref(0)

let mounted = false
let windowResizeTimer: number | null = null
let fullscreenChangeHandler: (() => void) | null = null

const playerContainerId = computed(() => `ezviz-player-${props.camera.id || 'tmp'}`)
const hasPlayer = computed(() => Boolean(player.value))

watch(
  () => props.camera.id,
  async (cameraId) => {
    if (!cameraId) {
      teardownPlayer()
      playConfig.value = null
      isConnected.value = false
      isLoading.value = false
      playFailed.value = false
      return
    }
    if (!mounted) return
    autoRetryCount.value = 0
    await bootstrapPlayer()
  }
)

const asApiResult = <T>(response: unknown) => response as ApiResult<T>

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const e = error as { response?: { data?: { message?: string } }; message?: string }
  return e.response?.data?.message || e.message || fallback
}

/** 等待播放器容器挂载并有有效宽度 */
const waitForHostReady = async () => {
  await nextTick()
  for (let i = 0; i < 20; i++) {
    const host = playerContainerRef.value
    const stage = stageRef.value
    if (host && stage && host.isConnected && stage.clientWidth > 0) {
      return true
    }
    await new Promise(r => setTimeout(r, 80))
  }
  return false
}

/** 根据容器宽度计算 16:9 尺寸 */
const computeSize = () => {
  const stage = stageRef.value
  const w = Math.max(stage?.clientWidth || 640, 200)
  const h = Math.round(w * 9 / 16)
  return { w, h }
}

/** 将计算出的尺寸写入 stage 和 host 的内联样式 */
const applyExplicitSize = () => {
  const stage = stageRef.value
  const host = playerContainerRef.value
  if (!stage || !host) return
  const { w, h } = computeSize()
  stage.style.height = `${h}px`
  host.style.width = `${w}px`
  host.style.height = `${h}px`
  player.value?.resize?.(w, h)
}

/** window resize 处理（防抖） */
const onWindowResize = () => {
  if (windowResizeTimer !== null) clearTimeout(windowResizeTimer)
  windowResizeTimer = window.setTimeout(applyExplicitSize, 300)
}

const clearFallbackTimer = () => {
  if (fallbackTimer.value !== null) {
    clearTimeout(fallbackTimer.value)
    fallbackTimer.value = null
  }
}

const teardownPlayer = () => {
  clearFallbackTimer()
  if (player.value) {
    player.value.destroy?.()
    player.value = null
  }
  if (windowResizeTimer !== null) {
    clearTimeout(windowResizeTimer)
    windowResizeTimer = null
  }
  window.removeEventListener('resize', onWindowResize)
  if (fullscreenChangeHandler) {
    document.removeEventListener('fullscreenchange', fullscreenChangeHandler)
    document.removeEventListener('webkitfullscreenchange', fullscreenChangeHandler as EventListener)
    fullscreenChangeHandler = null
  }
  isPlaying.value = true
}

const initSdkPlayer = () => {
  const host = playerContainerRef.value
  if (!host || !playConfig.value?.accessToken || !playConfig.value?.playUrl) {
    isLoading.value = false
    playFailed.value = true
    emit('statusChange', false)
    return
  }

  // 计算尺寸并写入内联样式（跟随萤石官方 demo 模式：显式像素）
  const { w, h } = computeSize()
  const stage = stageRef.value
  if (stage) stage.style.height = `${h}px`
  host.style.width = `${w}px`
  host.style.height = `${h}px`

  player.value = new EZUIKitPlayer({
    id: playerContainerId.value,
    accessToken: playConfig.value.accessToken,
    url: playConfig.value.playUrl,
    width: w,
    height: h,
    autoplay: true,
    audio: false,
    template: 'simple',
    loggerOptions: { name: 'ezuikit', level: 'LOG', showTime: true },
    handleSuccess: () => {
      console.log('[EZVIZ] handleSuccess — 播放成功')
      clearFallbackTimer()
      isLoading.value = false
      isConnected.value = true
      playFailed.value = false
      emit('statusChange', true)
    },
    handleError: (err: any) => {
      console.error('[EZVIZ] handleError — 播放错误:', err)
      clearFallbackTimer()
      isLoading.value = false
      isConnected.value = false
      playFailed.value = true
      emit('statusChange', false)
    }
  })

  // 窗口 resize（防抖，不使用 ResizeObserver）
  window.addEventListener('resize', onWindowResize)

  // 全屏切换后重算尺寸
  fullscreenChangeHandler = () => {
    setTimeout(() => {
      const stage = stageRef.value
      const host = playerContainerRef.value
      if (!stage || !host) return
      const isFS = !!(document.fullscreenElement || (document as any).webkitFullscreenElement)
      if (isFS) {
        const fw = window.innerWidth
        const fh = window.innerHeight
        stage.style.height = `${fh}px`
        host.style.width = `${fw}px`
        host.style.height = `${fh}px`
        player.value?.resize?.(fw, fh)
      } else {
        applyExplicitSize()
      }
      if (!isConnected.value && player.value) {
        player.value.play?.()
      }
    }, 200)
  }
  document.addEventListener('fullscreenchange', fullscreenChangeHandler)
  document.addEventListener('webkitfullscreenchange', fullscreenChangeHandler as EventListener)

  bindSdkEvents()
}

const bindSdkEvents = () => {
  const emitter = player.value?.eventEmitter
  const events = (EZUIKitPlayer as any).EVENTS

  if (!emitter || !events) {
    fallbackTimer.value = window.setTimeout(() => handleFirstFrameTimeout(), 5000)
    return
  }

  emitter.on(events.firstFrameDisplay, () => {
    clearFallbackTimer()
    isLoading.value = false
    isConnected.value = true
    playFailed.value = false
    emit('statusChange', true)
  })

  emitter.on(events.destroy, () => {
    isConnected.value = false
    emit('statusChange', false)
  })

  fallbackTimer.value = window.setTimeout(() => handleFirstFrameTimeout(), 5000)
}

const handleFirstFrameTimeout = async () => {
  if (isConnected.value) {
    isLoading.value = false
    emit('statusChange', true)
    return
  }
  if (autoRetryCount.value < 2) {
    autoRetryCount.value++
    await bootstrapPlayer({ silent: true })
    return
  }
  isLoading.value = false
  playFailed.value = true
  isConnected.value = false
  emit('statusChange', false)
}

const bootstrapPlayer = async (options: { silent?: boolean } = {}) => {
  if (!props.camera.id || !mounted) return

  teardownPlayer()
  isLoading.value = true
  isConnected.value = false
  playFailed.value = false
  isPlaying.value = true
  playConfig.value = null

  try {
    const res = asApiResult<CameraPlayConfig>(await getCameraPlayConfigAPI(props.camera.id))
    if (res.code !== 200 || !res.data) {
      throw new Error(res.message || '获取播放配置失败')
    }
    playConfig.value = res.data
    emit('loaded', res.data)
    console.log('[EZVIZ] playConfig:', {
      url: res.data.playUrl,
      token: res.data.accessToken ? '***已获取***' : '空',
      deviceSerial: res.data.deviceSerial,
      channelNo: res.data.channelNo
    })

    const ready = await waitForHostReady()
    if (!ready) throw new Error('播放器容器未就绪')

    initSdkPlayer()
  } catch (error) {
    isLoading.value = false
    isConnected.value = false
    playFailed.value = true
    emit('statusChange', false)
    if (!options.silent) {
      ElMessage.error(resolveErrorMessage(error, '播放器初始化失败'))
    }
    console.error(error)
  }
}

const reloadPlayer = async () => {
  autoRetryCount.value = 0
  await bootstrapPlayer()
}

const togglePlay = () => {
  if (!player.value) return
  if (isPlaying.value) {
    player.value.pause?.()
  } else {
    player.value.play?.()
  }
  isPlaying.value = !isPlaying.value
}

const toggleFullScreen = () => {
  const stage = stageRef.value
  if (!stage) return
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    stage.requestFullscreen()
  }
}

defineExpose({ reloadPlayer, isConnected, playConfig })

onMounted(() => {
  mounted = true
  if (props.camera.id) {
    isLoading.value = true
    bootstrapPlayer()
  }
})

onBeforeUnmount(() => {
  mounted = false
  teardownPlayer()
})
</script>

<style scoped>
.ez-player-wrap {
  width: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  overflow: hidden;
  background: #0d1117;
}

.player-stage {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: #0d1117;
}

.player-host {
  position: absolute;
  top: 0;
  left: 0;
}

:deep(.player-host > div) {
  position: relative !important;
  width: 100% !important;
  height: 100% !important;
  background: #0d1117 !important;
}

:deep(.player-host video),
:deep(.player-host iframe),
:deep(.player-host object),
:deep(.player-host embed),
:deep(.player-host canvas) {
  display: block !important;
  width: 100% !important;
  height: 100% !important;
  background: #0d1117 !important;
}

/* Hide EZUIKit's full-surface loading mask and keep loading feedback local to our overlay. */
:deep(.player-host .ezuikit-spin),
:deep(.player-host .ezuikit-spin-container::after),
:deep(.player-host .ezuikit-spin-blur::after) {
  display: none !important;
  opacity: 0 !important;
}

:deep(.player-host .ezuikit-spin-container),
:deep(.player-host .ezuikit-spin-blur) {
  opacity: 1 !important;
  pointer-events: auto !important;
  user-select: auto !important;
}

/* State overlays */
.state-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(13, 17, 23, 0.8);
  color: #8b949e;
  font-size: 13px;
  z-index: 8;
}

.state-overlay.failed {
  cursor: pointer;
}

.state-svg {
  width: 32px;
  height: 32px;
  color: #f85149;
}

.retry-btn {
  padding: 6px 18px;
  border-radius: 6px;
  border: 1px solid #30363d;
  background: #21262d;
  color: #c9d1d9;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: #30363d;
}

/* Bottom controls */
.player-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #161b22;
  border-top: 1px solid #21262d;
}

.ctrl-left,
.ctrl-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.live-badge {
  display: inline-flex;
  align-items: center;
  padding: 1px 6px;
  border-radius: 3px;
  background: #da3633;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.device-info {
  color: #8b949e;
  font-size: 11px;
  font-family: 'SF Mono', 'Cascadia Code', monospace;
}

.ctrl-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #8b949e;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.ctrl-btn svg {
  width: 16px;
  height: 16px;
}

.ctrl-btn:hover:not(:disabled) {
  background: #21262d;
  color: #c9d1d9;
}

.ctrl-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.on {
  background: #3fb950;
  box-shadow: 0 0 4px #3fb95080;
}

.status-dot.err {
  background: #f85149;
}

.status-dot.off {
  background: #484f58;
}
</style>
