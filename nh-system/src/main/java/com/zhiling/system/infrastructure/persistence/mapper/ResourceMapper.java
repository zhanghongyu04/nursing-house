package com.zhiling.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiling.model.entity.Resource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资源 Mapper
 *
 * @author zhanghongyu
 */
@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {

    /**
     * 根据角色ID获取资源（复杂查询，保留注解）
     */
    @Select("SELECT distinct r.* from sys_resource r left join sys_role_resource rr " +
            "on r.resource_no=rr.resource_no where rr.role_id=#{roleId} and rr.status=0 and r.status=0")
    List<Resource> getResourceByRoleId(Long roleId);
}

