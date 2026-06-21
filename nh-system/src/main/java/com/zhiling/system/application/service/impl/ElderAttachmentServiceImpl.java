package com.zhiling.system.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.model.entity.Elder;
import com.zhiling.model.entity.ElderAttachment;
import com.zhiling.system.application.service.CommonFileService;
import com.zhiling.system.application.service.ElderAttachmentService;
import com.zhiling.system.infrastructure.persistence.mapper.ElderAttachmentMapper;
import com.zhiling.system.infrastructure.persistence.mapper.ElderMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 老人档案附件服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class ElderAttachmentServiceImpl implements ElderAttachmentService {

    private static final int TYPE_IMAGE = 0;
    private static final int TYPE_FILE = 1;

    private final ElderAttachmentMapper elderAttachmentMapper;
    private final ElderMapper elderMapper;
    private final CommonFileService commonFileService;

    public ElderAttachmentServiceImpl(ElderAttachmentMapper elderAttachmentMapper,
                                      ElderMapper elderMapper,
                                      CommonFileService commonFileService) {
        this.elderAttachmentMapper = elderAttachmentMapper;
        this.elderMapper = elderMapper;
        this.commonFileService = commonFileService;
    }

    @Override
    public List<ElderAttachment> listByElderId(Long elderId) {
        if (elderId == null) {
            throw new ProjectException(400, "老人ID不能为空");
        }
        return elderAttachmentMapper.selectList(
                new LambdaQueryWrapper<ElderAttachment>()
                        .eq(ElderAttachment::getElderId, elderId)
                        .eq(ElderAttachment::getStatus, 0)
                        .orderByAsc(ElderAttachment::getAttachmentType)
                        .orderByDesc(ElderAttachment::getCreateTime)
        );
    }

    @Override
    public ElderAttachment upload(Long elderId, MultipartFile file, Integer attachmentType, String remark) {
        if (elderId == null) {
            throw new ProjectException(400, "老人ID不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new ProjectException(400, "上传文件不能为空");
        }
        Elder elder = elderMapper.selectById(elderId);
        if (elder == null) {
            throw new ProjectException(404, "老人档案不存在");
        }

        int normalizedType = normalizeType(attachmentType, file.getContentType());
        String fileUrl = commonFileService.upload(file);
        ElderAttachment attachment = ElderAttachment.builder()
                .elderId(elderId)
                .fileName(resolveFileName(file))
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .attachmentType(normalizedType)
                .build();
        attachment.setStatus(0);
        attachment.setRemark(remark);
        elderAttachmentMapper.insert(attachment);
        return attachment;
    }

    @Override
    public Boolean delete(Long id) {
        if (id == null) {
            throw new ProjectException(400, "附件ID不能为空");
        }
        ElderAttachment existing = elderAttachmentMapper.selectById(id);
        if (existing == null) {
            return true;
        }
        existing.setStatus(1);
        return elderAttachmentMapper.updateById(existing) > 0;
    }

    private int normalizeType(Integer requestedType, String contentType) {
        if (requestedType != null && (requestedType == TYPE_IMAGE || requestedType == TYPE_FILE)) {
            return requestedType;
        }
        return StringUtils.hasText(contentType) && contentType.toLowerCase().startsWith("image/")
                ? TYPE_IMAGE
                : TYPE_FILE;
    }

    private String resolveFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.hasText(originalFilename)) {
            return originalFilename;
        }
        return "未命名附件";
    }
}
