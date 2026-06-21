package com.zhiling.framework.security.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 当前请求访问范围模型。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessScope {

    private Long userId;
    private Long sanaId;
    private Set<Long> sanaScopeIds;
    private Set<String> roleLabels;
    private Set<String> resourcePaths;
}