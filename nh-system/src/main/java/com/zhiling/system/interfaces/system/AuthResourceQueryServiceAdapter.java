package com.zhiling.system.interfaces.system;

import com.zhiling.model.entity.Resource;
import com.zhiling.system.auth.service.support.AuthResourceQueryService;
import com.zhiling.system.application.service.ResourceService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 认证域资源查询适配器。
 *
 * @author zhanghongyu
 */
@Component
public class AuthResourceQueryServiceAdapter implements AuthResourceQueryService {

    private final ResourceService resourceService;

    /**
     * 构造器：AuthResourceQueryServiceAdapter
     *
     * @author zhanghongyu
     */
    public AuthResourceQueryServiceAdapter(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 方法：getResourceListByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Resource> getResourceListByUserId(String userId) {
        return resourceService.getResourceListByUserId(userId);
    }
}