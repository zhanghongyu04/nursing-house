package com.zhiling.system.application.service;

import com.zhiling.model.entity.Resource;

import java.util.List;

/**

 * ResourceService

 *

 * @author zhanghongyu

 */

public interface ResourceService {
    List<Resource> getResourceListByUserId(String id);
}



