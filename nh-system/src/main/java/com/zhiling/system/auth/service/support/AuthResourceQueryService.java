package com.zhiling.system.auth.service.support;

import com.zhiling.model.entity.Resource;

import java.util.List;

/**
 * 认证域资源查询服务。
 *
 * @author zhanghongyu
 */
public interface AuthResourceQueryService {

    List<Resource> getResourceListByUserId(String userId);
}