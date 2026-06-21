package com.zhiling.system.infrastructure.ezviz.service;

import com.zhiling.system.infrastructure.ezviz.client.EzvizOpenApiClient;
import com.zhiling.system.infrastructure.ezviz.config.EzvizProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
/**
 * EzvizTokenServiceImpl
 *
 * @author zhanghongyu
 */
public class EzvizTokenServiceImpl implements EzvizTokenService {

    private final EzvizOpenApiClient ezvizOpenApiClient;
    private final EzvizProperties ezvizProperties;
    private final EzvizCredentialService ezvizCredentialService;
    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile String accessToken;
    private volatile Instant expireAt;

    public EzvizTokenServiceImpl(EzvizOpenApiClient ezvizOpenApiClient,
                                 EzvizProperties ezvizProperties,
                                 EzvizCredentialService ezvizCredentialService) {
        this.ezvizOpenApiClient = ezvizOpenApiClient;
        this.ezvizProperties = ezvizProperties;
        this.ezvizCredentialService = ezvizCredentialService;
    }

    /**
     * 方法：getValidAccessToken
     *
     * @author zhanghongyu
     */
    @Override
    public String getValidAccessToken() {
        if (!needRefresh()) {
            return accessToken;
        }
        refreshAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("萤石 accessToken 获取失败");
        }
        return accessToken;
    }

    /**
     * 方法：refreshAccessToken
     *
     * @author zhanghongyu
     */
    @Override
    public void refreshAccessToken() {
        refreshLock.lock();
        try {
            if (!needRefresh()) {
                return;
            }
            EzvizOpenApiClient.TokenResponse tokenResponse = ezvizOpenApiClient.getAccessToken();
            if (tokenResponse == null || tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
                this.accessToken = null;
                this.expireAt = null;
                throw new IllegalStateException("萤石 accessToken 获取失败");
            }
            this.accessToken = tokenResponse.accessToken();
            this.expireAt = resolveExpireAt(tokenResponse.expireTime());
        } finally {
            refreshLock.unlock();
        }
    }

    /**
     * 方法：invalidateAccessToken
     *
     * @author zhanghongyu
     */
    @Override
    public void invalidateAccessToken() {
        refreshLock.lock();
        try {
            this.accessToken = null;
            this.expireAt = null;
        } finally {
            refreshLock.unlock();
        }
    }

    /**
     * 方法：needRefresh
     *
     * @author zhanghongyu
     */
    private boolean needRefresh() {
        if (!ezvizCredentialService.getCredential().isValid()) {
            return true;
        }
        if (accessToken == null || accessToken.isBlank() || expireAt == null) {
            return true;
        }
        Instant refreshAt = expireAt.minusSeconds(ezvizProperties.getTokenRefreshBeforeExpireSeconds());
        return Instant.now().isAfter(refreshAt);
    }

    /**
     * 萤石 token/get 返回的 expireTime 为毫秒级过期时间戳。
     * 兼容旧实现中可能被测试桩传入的有效秒数，避免本地缓存长期不刷新。
     */
    private Instant resolveExpireAt(long expireTime) {
        if (expireTime <= 0) {
            return Instant.now();
        }
        long epochMillisThreshold = TimeUnit.DAYS.toMillis(365);
        if (expireTime > epochMillisThreshold) {
            return Instant.ofEpochMilli(expireTime);
        }
        return Instant.now().plusSeconds(expireTime);
    }
}
