package com.zhiling.system.infrastructure.ezviz.service;

import com.zhiling.system.infrastructure.ezviz.config.EzvizCredential;
import com.zhiling.system.infrastructure.ezviz.config.EzvizProperties;
import com.zhiling.system.infrastructure.ezviz.mapper.EzvizConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
/**
 * EzvizCredentialService
 *
 * @author zhanghongyu
 */
public class EzvizCredentialService {

    private final EzvizProperties ezvizProperties;
    private final EzvizConfigMapper ezvizConfigMapper;

    public EzvizCredentialService(EzvizProperties ezvizProperties,
                                  EzvizConfigMapper ezvizConfigMapper) {
        this.ezvizProperties = ezvizProperties;
        this.ezvizConfigMapper = ezvizConfigMapper;
    }

    /**
     * 方法：getCredential
     *
     * @author zhanghongyu
     */
    public EzvizCredential getCredential() {
        if (ezvizProperties.isConfigured()) {
            return new EzvizCredential(ezvizProperties.getAppKey(), ezvizProperties.getAppSecret());
        }

        Map<String, Object> config = ezvizConfigMapper.selectActiveEzvizConfig();
        return new EzvizCredential(
                trimToNull(stringValue(config == null ? null : config.get("app_key"))),
                trimToNull(stringValue(config == null ? null : config.get("app_secret")))
        );
    }

    /**
     * 方法：stringValue
     *
     * @author zhanghongyu
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 方法：trimToNull
     *
     * @author zhanghongyu
     */
    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}