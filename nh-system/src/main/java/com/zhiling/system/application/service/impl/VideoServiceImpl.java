package com.zhiling.system.application.service.impl;

import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.system.application.dto.CameraPlayConfigDTO;
import com.zhiling.system.application.dto.VideoAlertHandleRequest;
import com.zhiling.system.application.dto.VideoPtzRequest;
import com.zhiling.model.entity.Camera;
import com.zhiling.model.entity.VideoAlert;
import com.zhiling.system.application.service.VideoService;
import com.zhiling.system.application.repository.SanatoriumRepository;
import com.zhiling.system.application.repository.VideoRepository;
import com.zhiling.system.infrastructure.ezviz.client.EzvizOpenApiClient;
import com.zhiling.system.infrastructure.ezviz.service.EzvizTokenService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Slf4j
@Service
/**
 * VideoServiceImpl
 *
 * @author zhanghongyu
 */
public class VideoServiceImpl implements VideoService {
    private static final String EZVIZ_PROTOCOL = "ezopen";
    private static final String VIDEO_LIST_PATH = "/web/video/list";
    private static final String VIDEO_ADD_PATH = "/web/video/add";
    private static final String VIDEO_UPDATE_PATH = "/web/video/update";
    private static final String VIDEO_DELETE_PATH = "/web/video/delete";
    private static final String VIDEO_ALERT_LIST_PATH = "/web/video/getAlerts";
    private static final String VIDEO_ALERT_HANDLE_PATH = "/web/video/handleAlert";
    private static final String VIDEO_PLAY_CONFIG_PATH = "/web/video/{cameraId}/play-config";
    private static final String VIDEO_PTZ_START_PATH = "/web/video/{cameraId}/ptz/start";
    private static final String VIDEO_PTZ_STOP_PATH = "/web/video/{cameraId}/ptz/stop";

    private final VideoRepository videoRepository;
    private final SanatoriumRepository sanatoriumRepository;
    private final SecurityHelper securityHelper;
    private final EzvizTokenService ezvizTokenService;
    private final EzvizOpenApiClient ezvizOpenApiClient;

    public VideoServiceImpl(VideoRepository videoRepository,
                           SanatoriumRepository sanatoriumRepository,
                           SecurityHelper securityHelper,
                           EzvizTokenService ezvizTokenService,
                           EzvizOpenApiClient ezvizOpenApiClient) {
        this.videoRepository = videoRepository;
        this.sanatoriumRepository = sanatoriumRepository;
        this.securityHelper = securityHelper;
        this.ezvizTokenService = ezvizTokenService;
        this.ezvizOpenApiClient = ezvizOpenApiClient;
    }


    /**
     * 方法：add
     *
     * @author zhanghongyu
     */
    public Boolean add(Camera camera) {
        requireResource("video.add", VIDEO_ADD_PATH);
        if (camera == null || camera.getSanaId() == null) {
            throw new IllegalArgumentException("机构ID不能为空");
        }
        requireSanaScope(camera.getSanaId(), "无权新增该机构摄像头");
        return videoRepository.insertCamera(camera);
    }

    /**
     * 方法：delete
     *
     * @author zhanghongyu
     */
    public Boolean delete(Long cameraId) {
        requireResource("video.delete", VIDEO_DELETE_PATH);
        requireAuthorizedCamera(cameraId);
        return videoRepository.deleteCameraById(cameraId);
    }

    /**
     * 方法：list
     *
     * @author zhanghongyu
     */
    public List<Camera> list(Long sanaId, String sanaName) {
        requireResource("video.list", VIDEO_LIST_PATH);
        Long targetSanaId = resolveTargetSanaId(sanaId, sanaName);
        if (targetSanaId == null) {
            return List.of();
        }
        return videoRepository.listCameraBySanaId(targetSanaId);
    }

    /**
     * 方法：update
     *
     * @author zhanghongyu
     */
    public Boolean update(Camera camera) {
        requireResource("video.update", VIDEO_UPDATE_PATH);
        if (camera == null || camera.getId() == null) {
            throw new IllegalArgumentException("摄像头ID不能为空");
        }
        Camera existing = requireAuthorizedCamera(camera.getId());
        Long newSanaId = camera.getSanaId();
        if (newSanaId != null && !newSanaId.equals(existing.getSanaId())) {
            requireSanaScope(newSanaId, "无权将摄像头调整到该机构");
        }
        return videoRepository.updateCameraById(camera);
    }

    /**
     * 方法：getAlerts
     *
     * @author zhanghongyu
     */
    public List<VideoAlert> getAlerts(Long sanaId, String sanaName) {
        requireResource("video.getAlerts", VIDEO_ALERT_LIST_PATH);
        Long targetSanaId = resolveTargetSanaId(sanaId, sanaName);
        if (targetSanaId == null) {
            return List.of();
        }
        return videoRepository.listAlertsBySanaId(targetSanaId);
    }

    /**
     * 方法：handleAlert
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean handleAlert(VideoAlertHandleRequest request) {
        requireResource("video.handleAlert", VIDEO_ALERT_HANDLE_PATH);
        if (request == null || request.getAlertId() == null) {
            throw new IllegalArgumentException("告警ID不能为空");
        }

        VideoAlert alert = videoRepository.getAlertById(request.getAlertId());
        if (alert == null) {
            throw new IllegalArgumentException("告警不存在");
        }
        requireSanaScope(alert.getSanaId(), "无权处理该机构告警");
        if (alert.getStatus() != null && alert.getStatus() == 1) {
            return Boolean.TRUE;
        }

        Long currentUserId = securityHelper.requireCurrentUserId();
        return videoRepository.handleAlert(alert.getId(), currentUserId, trimToNull(request.getHandleRemark()));
    }

    /**
     * 方法：getPlayConfig
     *
     * @author zhanghongyu
     */
    @Override
    public CameraPlayConfigDTO getPlayConfig(Long cameraId) {
        requireResource("video.playConfig", VIDEO_PLAY_CONFIG_PATH);
        Camera camera = requireAuthorizedCamera(cameraId);
        requireEzvizMapping(camera);
        TokenCallResult<EzvizOpenApiClient.LiveAddressResponse> tokenCallResult = callWithValidEzvizToken(accessToken ->
                ezvizOpenApiClient.getLiveAddress(
                        accessToken,
                        camera.getDeviceSerial(),
                        camera.getChannelNo(),
                        1,
                        camera.getValidateCode(),
                        resolveQuality(camera.getStreamQuality())
                )
        );
        String accessToken = tokenCallResult.accessToken();
        EzvizOpenApiClient.LiveAddressResponse liveAddressResponse = tokenCallResult.data();
        if (liveAddressResponse == null) {
            log.warn("[VideoService] 摄像头 {} ({}) 获取播放地址失败，设备可能离线",
                    camera.getId(), camera.getDeviceSerial());
            return new CameraPlayConfigDTO(
                    camera.getId(),
                    camera.getCameraName(),
                    camera.getDeviceSerial(),
                    camera.getChannelNo(),
                    EZVIZ_PROTOCOL,
                    camera.getStreamQuality(),
                    null,
                    accessToken,
                    camera.getCameraStatus(),
                    camera.getOnlineStatus()
            );
        }
        return new CameraPlayConfigDTO(
                camera.getId(),
                camera.getCameraName(),
                camera.getDeviceSerial(),
                camera.getChannelNo(),
                EZVIZ_PROTOCOL,
                camera.getStreamQuality(),
                liveAddressResponse.url(),
                accessToken,
                camera.getCameraStatus(),
                camera.getOnlineStatus()
        );
    }

    /**
     * 方法：startPtz
     *
     * @author zhanghongyu
     */
    @Override
    public void startPtz(Long cameraId, VideoPtzRequest request) {
        requireResource("video.ptz.start", VIDEO_PTZ_START_PATH);
        Camera camera = requireAuthorizedCamera(cameraId);
        requireEzvizMapping(camera);
        if (request == null || request.getDirection() == null) {
            throw new IllegalArgumentException("云台方向不能为空");
        }
        int speed = request.getSpeed() == null ? 2 : request.getSpeed();
        callWithValidEzvizToken(accessToken -> {
            ezvizOpenApiClient.startPtz(accessToken, camera.getDeviceSerial(), camera.getChannelNo(), request.getDirection(), speed);
            return null;
        });
    }

    /**
     * 方法：stopPtz
     *
     * @author zhanghongyu
     */
    @Override
    public void stopPtz(Long cameraId) {
        requireResource("video.ptz.stop", VIDEO_PTZ_STOP_PATH);
        Camera camera = requireAuthorizedCamera(cameraId);
        requireEzvizMapping(camera);
        callWithValidEzvizToken(accessToken -> {
            ezvizOpenApiClient.stopPtz(accessToken, camera.getDeviceSerial(), camera.getChannelNo());
            return null;
        });
    }

    /**
     * 方法：syncOnlineStatus
     *
     * @author zhanghongyu
     */
    @Override
    public int syncOnlineStatus() {
        Map<String, Integer> deviceStatusMap = callWithValidEzvizToken(ezvizOpenApiClient::getDeviceOnlineStatus).data();
        if (deviceStatusMap.isEmpty()) {
            log.warn("[VideoSync] 萤石设备列表为空，跳过同步");
            return 0;
        }

        List<Camera> cameras = videoRepository.listAllWithDeviceSerial();
        int updated = 0;
        for (Camera camera : cameras) {
            Integer newStatus = deviceStatusMap.get(camera.getDeviceSerial());
            if (newStatus == null) {
                continue;
            }
            Integer oldStatus = camera.getOnlineStatus();
            if (!newStatus.equals(oldStatus)) {
                camera.setOnlineStatus(newStatus);
                videoRepository.updateCameraById(camera);
                updated++;

                String content = buildStatusChangeContent(newStatus);
                videoRepository.insertStatusLog(camera.getSanaId(), camera.getId(), content);
                log.info("[VideoSync] 摄像头 {} ({}) 状态变更: {} -> {}, 记录已写入",
                        camera.getId(), camera.getDeviceSerial(), oldStatus, newStatus);
            }
        }
        log.info("[VideoSync] 同步完成, 共 {} 个摄像头, 更新 {} 个", cameras.size(), updated);
        return updated;
    }

    /**
     * 方法：buildStatusChangeContent
     *
     * @author zhanghongyu
     */
    private String buildStatusChangeContent(int newStatus) {
        return switch (newStatus) {
            case 1 -> "设备已上线";
            case 0 -> "设备已离线";
            case 2 -> "设备异常";
            default -> "设备状态变更";
        };
    }

    /**
     * 方法：requireResource
     *
     * @author zhanghongyu
     */
    private void requireResource(String scene, String resourcePath) {
        if (!securityHelper.hasResourcePathForSensitiveOperation(resourcePath)) {
            log.warn("[VideoPermission] 用户 {} 缺少视频监控权限 scene={}, path={}",
                    securityHelper.getCurrentUserId(), scene, resourcePath);
            throw new AccessDeniedException("无权访问视频监控功能");
        }
    }

    /**
     * 方法：resolveTargetSanaId
     *
     * @author zhanghongyu
     */
    private Long resolveTargetSanaId(Long sanaId, String sanaName) {
        validateSanaSelector(sanaId, sanaName);
        if (sanaId != null) {
            return resolveTargetSanaIdById(sanaId);
        }
        return resolveTargetSanaIdByName(sanaName);
    }

    /**
     * 方法：resolveTargetSanaIdById
     *
     * @author zhanghongyu
     */
    private Long resolveTargetSanaIdById(Long sanaId) {
        if (securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            return sanaId;
        }
        Set<Long> sanaScopeIds = securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
        if (!sanaScopeIds.contains(sanaId)) {
            throw new AccessDeniedException("无权查看该机构视频数据");
        }
        return sanaId;
    }

    /**
     * 方法：resolveTargetSanaIdByName
     *
     * @author zhanghongyu
     */
    private Long resolveTargetSanaIdByName(String sanaName) {
        if (securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            if (sanaName == null || sanaName.trim().isEmpty()) {
                return null;
            }
            return sanatoriumRepository.selectIdByName(sanaName.trim());
        }

        Set<Long> sanaScopeIds = securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
        if (sanaName == null || sanaName.trim().isEmpty()) {
            if (sanaScopeIds.size() == 1) {
                return sanaScopeIds.iterator().next();
            }
            throw new AccessDeniedException("当前账号已授权多个机构，请明确指定机构");
        }
        Long targetSanaId = sanatoriumRepository.selectIdByName(sanaName.trim());
        if (targetSanaId == null || !sanaScopeIds.contains(targetSanaId)) {
            throw new AccessDeniedException("无权查看该机构视频数据");
        }
        return targetSanaId;
    }

    /**
     * 方法：validateSanaSelector
     *
     * @author zhanghongyu
     */
    private void validateSanaSelector(Long sanaId, String sanaName) {
        if (sanaId == null || sanaName == null || sanaName.trim().isEmpty()) {
            return;
        }
        Long sanaIdByName = sanatoriumRepository.selectIdByName(sanaName.trim());
        if (sanaIdByName == null) {
            throw new IllegalArgumentException("机构不存在");
        }
        if (!sanaId.equals(sanaIdByName)) {
            throw new IllegalArgumentException("sanaId 与 sanaName 不一致");
        }
    }

    /**
     * 方法：requireAuthorizedCamera
     *
     * @author zhanghongyu
     */
    private Camera requireAuthorizedCamera(Long cameraId) {
        Camera camera = videoRepository.getCameraById(cameraId);
        if (camera == null) {
            throw new IllegalArgumentException("摄像头不存在");
        }
        requireSanaScope(camera.getSanaId(), "无权操作该机构摄像头");
        return camera;
    }

    /**
     * 方法：requireSanaScope
     *
     * @author zhanghongyu
     */
    private void requireSanaScope(Long sanaId, String message) {
        if (!securityHelper.canOperateSanaForSensitiveOperation(sanaId)) {
            throw new AccessDeniedException(message);
        }
    }

    /**
     * 方法：requireEzvizMapping
     *
     * @author zhanghongyu
     */
    private void requireEzvizMapping(Camera camera) {
        if (camera.getDeviceSerial() == null || camera.getDeviceSerial().isBlank()) {
            throw new IllegalStateException("摄像头未配置萤石 deviceSerial");
        }
        if (camera.getChannelNo() == null || camera.getChannelNo() <= 0) {
            throw new IllegalStateException("摄像头未配置有效的萤石通道号");
        }
    }

    /**
     * 方法：resolveQuality
     *
     * @author zhanghongyu
     */
    private Integer resolveQuality(String streamQuality) {
        if (streamQuality == null || streamQuality.isBlank()) {
            return 1;
        }
        return "sd".equalsIgnoreCase(streamQuality) ? 2 : 1;
    }

    /**
     * 方法：trimToNull
     *
     * @author zhanghongyu
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 使用本地缓存 token 调用萤石接口；若平台明确返回 10002，则清空缓存并重试一次。
     */
    private <T> TokenCallResult<T> callWithValidEzvizToken(Function<String, T> operation) {
        String accessToken = ezvizTokenService.getValidAccessToken();
        try {
            return new TokenCallResult<>(accessToken, operation.apply(accessToken));
        } catch (EzvizOpenApiClient.EzvizTokenInvalidException ex) {
            log.warn("[VideoService] 萤石 accessToken 已失效，刷新后重试一次: {}", ex.getMessage());
            ezvizTokenService.invalidateAccessToken();
            String refreshedToken = ezvizTokenService.getValidAccessToken();
            return new TokenCallResult<>(refreshedToken, operation.apply(refreshedToken));
        }
    }

    private record TokenCallResult<T>(String accessToken, T data) {
    }

}
