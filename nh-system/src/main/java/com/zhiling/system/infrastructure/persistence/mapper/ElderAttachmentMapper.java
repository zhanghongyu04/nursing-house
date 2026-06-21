package com.zhiling.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiling.model.entity.ElderAttachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 老人档案附件 Mapper。
 *
 * @author zhanghongyu
 */
@Mapper
public interface ElderAttachmentMapper extends BaseMapper<ElderAttachment> {
}
