package com.zhiling.system.infrastructure.ezviz.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.Map;

@Mapper
/**
 * EzvizConfigMapper
 *
 * @author zhanghongyu
 */
public interface EzvizConfigMapper {

    @Select("""
            SELECT app_key, app_secret
            FROM tb_third_platform_config
            WHERE platform_code = 'EZVIZ' AND status = 0
            ORDER BY id ASC
            LIMIT 1
            """)
    Map<String, Object> selectActiveEzvizConfig();
}

