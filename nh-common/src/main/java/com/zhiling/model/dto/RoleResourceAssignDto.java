package com.zhiling.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 角色资源授权保存参数。
 *
 * @author zhanghongyu
 */
@Data
public class RoleResourceAssignDto {
    /**
     * 授权资源编号集合。
     */
    private List<String> resourceNos;
}
