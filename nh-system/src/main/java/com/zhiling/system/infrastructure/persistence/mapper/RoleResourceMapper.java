package com.zhiling.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiling.model.entity.RoleResource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色资源关联 Mapper。
 *
 * @author zhanghongyu
 */
@Mapper
public interface RoleResourceMapper extends BaseMapper<RoleResource> {
}
