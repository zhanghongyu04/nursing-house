package com.zhiling.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiling.model.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志 Mapper。
 *
 * @author zhanghongyu
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
