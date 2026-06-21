package com.zhiling.system.application.service;

import com.zhiling.model.entity.ElderAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 老人档案附件服务。
 *
 * @author zhanghongyu
 */
public interface ElderAttachmentService {

    List<ElderAttachment> listByElderId(Long elderId);

    ElderAttachment upload(Long elderId, MultipartFile file, Integer attachmentType, String remark);

    Boolean delete(Long id);
}
