package com.zhiling.system.infrastructure.ezviz.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.system.infrastructure.ezviz.config.EzvizCredential;
import com.zhiling.system.infrastructure.ezviz.config.EzvizProperties;
import com.zhiling.system.infrastructure.ezviz.service.EzvizCredentialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * 萤石开放平台 HTTP 客户端。
 * 封装 accessToken、云台控制、设备状态和直播地址等核心调用。
 *
 * @author zhanghongyu
 */
@Slf4j
@Component
public class EzvizOpenApiClient {

    private static final String TOKEN_INVALID_CODE = "10002";

    private final EzvizProperties ezvizProperties;
    private final EzvizCredentialService ezvizCredentialService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public EzvizOpenApiClient(EzvizProperties ezvizProperties,
                              EzvizCredentialService ezvizCredentialService,
                              ObjectMapper objectMapper) {
        this.ezvizProperties = ezvizProperties;
        this.ezvizCredentialService = ezvizCredentialService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(ezvizProperties.getConnectTimeoutMs()))
                .build();
    }

    /**
     * 方法：getAccessToken
     *
     * @author zhanghongyu
     */
    public TokenResponse getAccessToken() {
        EzvizCredential credential = requireCredential();
        Map<String, String> form = new LinkedHashMap<>();
        form.put("appKey", credential.appKey());
        form.put("appSecret", credential.appSecret());
        JsonNode data = postForm("/api/lapp/token/get", form, "getAccessToken");
        if (data == null) {
            log.error("[EZVIZ][getAccessToken] 获取 accessToken 失败");
            return null;
        }
        String token = textValue(data, "accessToken");
        long expireTime = data.path("expireTime").asLong(0L);
        if (token == null || token.isBlank() || expireTime <= 0) {
            log.error("[EZVIZ][getAccessToken] accessToken 响应缺少必要字段");
            return null;
        }
        return new TokenResponse(token, expireTime);
    }

    /**
     * 方法：startPtz
     *
     * @author zhanghongyu
     */
    public void startPtz(String accessToken, String deviceSerial, Integer channelNo, Integer direction, Integer speed) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("accessToken", accessToken);
        form.put("deviceSerial", deviceSerial);
        form.put("channelNo", String.valueOf(channelNo));
        form.put("direction", String.valueOf(direction));
        form.put("speed", String.valueOf(speed));
        postFormForPtz("/api/lapp/device/ptz/start", form, "startPtz", direction);
    }

    /**
     * 方法：stopPtz
     *
     * @author zhanghongyu
     */
    public void stopPtz(String accessToken, String deviceSerial, Integer channelNo) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("accessToken", accessToken);
        form.put("deviceSerial", deviceSerial);
        form.put("channelNo", String.valueOf(channelNo));
        postFormForPtz("/api/lapp/device/ptz/stop", form, "stopPtz", null);
    }

    /**
     * 查询设备列表及其在线状态。
     * 调用萤石 /api/lapp/device/list 接口，返回 deviceSerial → 在线状态 的映射。
     * 在线状态值：0=离线，1=在线，2=故障。
     */
    public Map<String, Integer> getDeviceOnlineStatus(String accessToken) {
        Map<String, Integer> statusMap = new LinkedHashMap<>();
        int pageStart = 0;
        int pageSize = 50;
        while (true) {
            // 萤石设备列表接口按页返回，这里持续翻页直到数据取完。
            Map<String, String> form = new LinkedHashMap<>();
            form.put("accessToken", accessToken);
            form.put("pageStart", String.valueOf(pageStart));
            form.put("pageSize", String.valueOf(pageSize));
            JsonNode data = postForm("/api/lapp/device/list", form, "getDeviceList");
            if (data == null) {
                break;
            }
            if (!data.isArray()) {
                break;
            }
            ArrayNode arr = (ArrayNode) data;
            if (arr.isEmpty()) {
                break;
            }
            for (JsonNode item : arr) {
                String serial = textValue(item, "deviceSerial");
                int status = item.path("status").asInt(0);
                if (serial != null && !serial.isBlank()) {
                    statusMap.put(serial, status);
                }
            }
            if (arr.size() < pageSize) {
                break;
            }
            pageStart++;
        }
        log.info("[EZVIZ][getDeviceOnlineStatus] 查询到 {} 个设备在线状态", statusMap.size());
        return statusMap;
    }

    public LiveAddressResponse getLiveAddress(String accessToken,
                                              String deviceSerial,
                                              Integer channelNo,
                                              Integer protocol,
                                              String code,
                                              Integer quality) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("accessToken", accessToken);
        form.put("deviceSerial", deviceSerial);
        form.put("channelNo", String.valueOf(channelNo));
        form.put("protocol", String.valueOf(protocol));
        if (code != null && !code.isBlank()) {
            form.put("code", code);
        }
        if (quality != null) {
            form.put("quality", String.valueOf(quality));
        }
        JsonNode data = postForm("/api/lapp/v2/live/address/get", form, "getLiveAddress");
        if (data == null) {
            return null;
        }
        String url = textValue(data, "url");
        String expireTime = textValue(data, "expireTime");
        String id = textValue(data, "id");
        if (url == null || url.isBlank()) {
            log.warn("[EZVIZ][getLiveAddress] 播放地址响应缺少 url 字段");
            return null;
        }
        return new LiveAddressResponse(id, url, expireTime);
    }

    /**
     * 统一封装萤石表单 POST 调用（查询类接口）。
     * 失败时记录日志并返回 null，不抛异常。
     */
    private JsonNode postForm(String path, Map<String, String> form, String apiName) {
        return postForm(path, form, apiName, null);
    }

    /**
     * 方法：postForm
     *
     * @author zhanghongyu
     */
    private JsonNode postForm(String path, Map<String, String> form, String apiName, Integer ptzDirection) {
        String body = toFormBody(form);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ezvizProperties.getBaseUrl() + path))
                .timeout(Duration.ofMillis(ezvizProperties.getReadTimeoutMs()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        long start = System.currentTimeMillis();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long durationMs = System.currentTimeMillis() - start;
            JsonNode root = objectMapper.readTree(response.body());
            String code = textValue(root, "code");
            String msg = textValue(root, "msg");
            log.info("[EZVIZ][{}] code={} msg={} durationMs={}", apiName, code, msg, durationMs);
            if (!isSuccess(code)) {
                log.warn("[EZVIZ][{}] 调用失败: code={}, msg={}", apiName, code, msg);
                if (TOKEN_INVALID_CODE.equals(code)) {
                    throw new EzvizTokenInvalidException(msg);
                }
                return null;
            }
            return root.path("data");
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("[EZVIZ][{}] 调用异常: {}", apiName, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * PTZ 云台控制专用调用。
     * 云台触达极限时抛出 ProjectException 给前端友好提示，其他失败仅记录日志。
     */
    private void postFormForPtz(String path, Map<String, String> form, String apiName, Integer ptzDirection) {
        String body = toFormBody(form);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ezvizProperties.getBaseUrl() + path))
                .timeout(Duration.ofMillis(ezvizProperties.getReadTimeoutMs()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        long start = System.currentTimeMillis();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long durationMs = System.currentTimeMillis() - start;
            JsonNode root = objectMapper.readTree(response.body());
            String code = textValue(root, "code");
            String msg = textValue(root, "msg");
            log.info("[EZVIZ][{}] code={} msg={} durationMs={}", apiName, code, msg, durationMs);
            if (!isSuccess(code)) {
                if (isPtzLimitMessage(msg)) {
                    throw new ProjectException(409, normalizePtzLimitMessage(ptzDirection, msg));
                }
                log.warn("[EZVIZ][{}] PTZ调用失败: code={}, msg={}", apiName, code, msg);
                if (TOKEN_INVALID_CODE.equals(code)) {
                    throw new EzvizTokenInvalidException(msg);
                }
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("[EZVIZ][{}] PTZ调用异常: {}", apiName, ex.getMessage(), ex);
        }
    }

    /**
     * 方法：requireCredential
     *
     * @author zhanghongyu
     */
    private EzvizCredential requireCredential() {
        EzvizCredential credential = ezvizCredentialService.getCredential();
        if (!credential.isValid()) {
            throw new IllegalStateException("未配置萤石 appKey/appSecret");
        }
        return credential;
    }

    /**
     * 方法：isSuccess
     *
     * @author zhanghongyu
     */
    private boolean isSuccess(String code) {
        return "200".equals(code) || "60000".equals(code) || "saas-200".equalsIgnoreCase(code);
    }

    /**
     * 方法：isPtzLimitMessage
     *
     * @author zhanghongyu
     */
    private boolean isPtzLimitMessage(String msg) {
        return msg != null && (msg.contains("限位") || msg.contains("极限"));
    }

    /**
     * 方法：normalizePtzLimitMessage
     *
     * @author zhanghongyu
     */
    private String normalizePtzLimitMessage(Integer direction, String msg) {
        String directionLabel = switch (direction == null ? -1 : direction) {
            case 0 -> "上方";
            case 1 -> "下方";
            case 2 -> "左侧";
            case 3 -> "右侧";
            case 4 -> "左上方";
            case 5 -> "左下方";
            case 6 -> "右上方";
            case 7 -> "右下方";
            default -> null;
        };
        String actionLabel = switch (direction == null ? -1 : direction) {
            case 0 -> "向上旋转";
            case 1 -> "向下旋转";
            case 2 -> "向左旋转";
            case 3 -> "向右旋转";
            case 4 -> "向左上旋转";
            case 5 -> "向左下旋转";
            case 6 -> "向右上旋转";
            case 7 -> "向右下旋转";
            default -> null;
        };
        if (directionLabel != null && actionLabel != null) {
            return "云台已到达" + directionLabel + "极限，无法继续" + actionLabel;
        }
        return normalizePtzLimitMessageByMessage(msg);
    }

    /**
     * 方法：normalizePtzLimitMessageByMessage
     *
     * @author zhanghongyu
     */
    private String normalizePtzLimitMessageByMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return "云台已到达当前方向极限";
        }
        if (msg.contains("右")) {
            return "云台已到达右侧极限，无法继续向右旋转";
        }
        if (msg.contains("左")) {
            return "云台已到达左侧极限，无法继续向左旋转";
        }
        if (msg.contains("上")) {
            return "云台已到达上方极限，无法继续向上旋转";
        }
        if (msg.contains("下")) {
            return "云台已到达下方极限，无法继续向下旋转";
        }
        return "云台已到达当前方向极限";
    }

    /**
     * 方法：textValue
     *
     * @author zhanghongyu
     */
    private String textValue(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    /**
     * 方法：toFormBody
     *
     * @author zhanghongyu
     */
    private String toFormBody(Map<String, String> form) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : form.entrySet()) {
            joiner.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return joiner.toString();
    }

    /**
     * 方法：TokenResponse
     *
     * @author zhanghongyu
     */
    public record TokenResponse(String accessToken, long expireTime) {
    }

    /**
     * 方法：LiveAddressResponse
     *
     * @author zhanghongyu
     */
    public record LiveAddressResponse(String id, String url, String expireTime) {
    }

    /**
     * 萤石明确返回 accessToken 失效时抛出，业务侧据此清空缓存并重试一次。
     */
    public static class EzvizTokenInvalidException extends RuntimeException {

        public EzvizTokenInvalidException(String message) {
            super(message == null || message.isBlank() ? "萤石 accessToken 已失效" : message);
        }
    }

}
