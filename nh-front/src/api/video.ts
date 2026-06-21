import httpInstance from "@/utils/http.ts";

type CameraId = number | string;
type AnyPayload = Record<string, any>;
type SanaId = number | string;

export interface VideoQuerySelector {
    sanaId?: SanaId;
    sanaName?: string;
}

export interface CameraItem {
    id?: number;
    sanaId: number;
    sanaName?: string;
    cameraName: string;
    cameraLocation: string;
    cameraStatus: number;
    deviceSerial?: string;
    channelNo?: number;
    validateCode?: string;
    streamQuality?: string;
    onlineStatus?: number;
    createTime?: string;
    updateTime?: string;
}

export interface VideoAlertItem {
    id?: number;
    sanaId: number;
    sanaName?: string;
    cameraId: number;
    cameraName?: string;
    cameraLocation?: string;
    content: string;
    alertTime: string;
    status: number;
    handledBy?: number;
    handledTime?: string;
    handleRemark?: string;
}

export interface CameraPlayConfig {
    cameraId: number;
    cameraName: string;
    deviceSerial: string;
    channelNo: number;
    protocol: string;
    streamQuality: string;
    playUrl: string;
    accessToken: string;
    cameraStatus: number;
    onlineStatus: number;
}

export interface VideoPtzRequest {
    direction: number;
    speed?: number;
}

export interface HandleVideoAlertRequest {
    alertId: number;
    handleRemark?: string;
}

// 添加视频监控
export const addCameraAPI = (data: AnyPayload) => {
    return httpInstance({
        url: "/api/v1/video/add",
        method: "POST",
        data
    });
};

// 删除视频监控
export const deleteCameraAPI = (cameraId: CameraId) => {
    return httpInstance({
        url: "/api/v1/video/delete",
        method: "DELETE",
        params: { cameraId }
    });
};

// 更新视频监控
export const updateCameraAPI = (data: AnyPayload) => {
    return httpInstance({
        url: "/api/v1/video/update",
        method: "POST",
        data
    });
};

// 获取视频监控列表
export const getCameraListAPI = (selector: VideoQuerySelector) => {
    return httpInstance({
        url: "/api/v1/video/list",
        method: "GET",
        params: selector
    });
};

// 获取视频监控告警
export const getVideoAlertsAPI = (selector: VideoQuerySelector) => {
    return httpInstance({
        url: "/api/v1/video/getAlerts",
        method: "GET",
        params: selector
    });
};

// 处理视频告警
export const handleVideoAlertAPI = (data: HandleVideoAlertRequest) => {
    return httpInstance({
        url: "/api/v1/video/handleAlert",
        method: "POST",
        data
    });
};

// 获取播放配置
export const getCameraPlayConfigAPI = (cameraId: CameraId) => {
    return httpInstance({
        url: `/api/v1/video/${cameraId}/play-config`,
        method: "GET"
    });
};

// 开始云台控制
export const startCameraPtzAPI = (cameraId: CameraId, data: VideoPtzRequest) => {
    return httpInstance({
        url: `/api/v1/video/${cameraId}/ptz/start`,
        method: "POST",
        data,
        silentErrorMessage: true
    } as any);
};

// 停止云台控制
export const stopCameraPtzAPI = (cameraId: CameraId) => {
    return httpInstance({
        url: `/api/v1/video/${cameraId}/ptz/stop`,
        method: "POST",
        silentErrorMessage: true
    } as any);
};
