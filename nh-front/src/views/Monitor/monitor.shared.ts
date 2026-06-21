/**
 * 监控模块共享工具函数
 */

export type CameraRuntimeStatus = 'online' | 'offline' | 'fault'

/**
 * 标准化摄像头数据，补全缺失字段
 */
export function normalizeCamera(raw: Record<string, any>) {
  return {
    id: raw.id ?? null,
    sanaId: raw.sanaId ?? 0,
    sanaName: raw.sanaName ?? '',
    cameraName: raw.cameraName ?? '',
    cameraLocation: raw.cameraLocation ?? '',
    cameraStatus: raw.cameraStatus ?? 0,
    deviceSerial: raw.deviceSerial ?? '',
    channelNo: raw.channelNo ?? 1,
    validateCode: raw.validateCode ?? '',
    streamQuality: raw.streamQuality ?? 'HD',
    onlineStatus: raw.onlineStatus ?? 0,
    createTime: raw.createTime ?? '',
    updateTime: raw.updateTime ?? ''
  }
}

/**
 * 根据字段值推断运行时状态
 *
 * cameraStatus: 0=正常, 1=故障, 2=离线
 * onlineStatus: 0=离线, 1=在线, 2=故障（来自萤石平台）
 */
export function getCameraRuntimeStatus(camera: {
  cameraStatus?: number
  onlineStatus?: number
}): CameraRuntimeStatus {
  if (camera.cameraStatus === 2) return 'offline'
  if (camera.onlineStatus === 0) return 'offline'
  if (camera.cameraStatus === 1 || camera.onlineStatus === 2) return 'fault'
  return 'online'
}

export function formatProtocol(val?: string): string {
  if (!val) return '--'
  const map: Record<string, string> = { ezopen: 'EZOPEN', hls: 'HLS', rtmp: 'RTMP', rtsp: 'RTSP' }
  return map[val.toLowerCase()] ?? val.toUpperCase()
}

export function formatStreamQuality(val?: string): string {
  if (!val) return '高清'
  const map: Record<string, string> = { HD: '高清', SD: '标清', LD: '流畅' }
  return map[val] ?? val
}

export function formatDateTime(val?: string): string {
  if (!val) return '--'
  try {
    return new Date(val).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch {
    return val
  }
}

export function formatLatestTime(val?: string): string {
  if (!val) return '--'
  try {
    const diff = Date.now() - new Date(val).getTime()
    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
    if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
    return formatDateTime(val)
  } catch {
    return val
  }
}
