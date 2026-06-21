package com.zhiling.framework.file;

import java.util.Map;

/**
 * RustFS 健康检查服务。
 *
 * @author zhanghongyu
 */
public interface RustFsHealthService {

    Map<String, Object> health();

    Map<String, String> config();
}