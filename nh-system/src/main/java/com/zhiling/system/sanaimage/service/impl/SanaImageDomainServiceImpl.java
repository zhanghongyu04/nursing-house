package com.zhiling.system.sanaimage.service.impl;

import com.zhiling.common.result.PageResult;
import com.zhiling.common.utils.FileUtil;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.model.dto.SanaImagePageQueryDto;
import com.zhiling.model.entity.SanaImage;
import com.zhiling.system.application.service.CommonFileService;
import com.zhiling.system.sanaimage.service.SanaImageDomainService;
import com.zhiling.system.sanaimage.service.SanaImageQueryService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 机构图片域服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class SanaImageDomainServiceImpl implements SanaImageDomainService {
    private static final String SANA_IMAGE_PAGE_PATH = "/web/sanaImage/page";
    private static final String SANA_IMAGE_ADD_PATH = "/web/sanaImage/add";
    private static final String SANA_IMAGE_DELETE_PATH = "/web/sanaImage/delete";

    private final SanaImageQueryService sanaImageQueryService;
    private final CommonFileService commonFileService;
    private final FileUtil fileUtil;
    private final SecurityHelper securityHelper;

    public SanaImageDomainServiceImpl(SanaImageQueryService sanaImageQueryService,
                                      CommonFileService commonFileService,
                                      FileUtil fileUtil,
                                      SecurityHelper securityHelper) {
        this.sanaImageQueryService = sanaImageQueryService;
        this.commonFileService = commonFileService;
        this.fileUtil = fileUtil;
        this.securityHelper = securityHelper;
    }

    /**
     * 方法：save
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean save(SanaImage sanaImage) {
        requireResource(SANA_IMAGE_ADD_PATH);
        Long sanaId = sanaImageQueryService.selectSanaIdByName(sanaImage.getSanaName());
        assertCanOperateSana(sanaId);
        sanaImage.setSanaId(sanaId);
        return sanaImageQueryService.save(sanaImage);
    }

    /**
     * 方法：remove
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean remove(SanaImage sanaImage) {
        requireResource(SANA_IMAGE_DELETE_PATH);
        Long sanaId = sanaImageQueryService.selectSanaIdByName(sanaImage.getSanaName());
        if (sanaId == null && sanaImage.getImageUrl() != null) {
            SanaImage existing = sanaImageQueryService.selectByImageUrl(sanaImage.getImageUrl());
            sanaId = existing == null ? null : existing.getSanaId();
        }
        assertCanOperateSana(sanaId);
        String fileName = fileUtil.extractFileNameFromUrl(sanaImage.getImageUrl());
        Boolean deletedFile = commonFileService.delete(fileName);
        Boolean deletedRecord = sanaImageQueryService.removeByImageUrl(sanaImage.getImageUrl());
        return deletedFile && deletedRecord;
    }

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @Override
    public PageResult page(SanaImagePageQueryDto sanaImage) {
        requireResource(SANA_IMAGE_PAGE_PATH);
        Long sanaId = sanaImageQueryService.selectSanaIdByName(sanaImage.getSanaName());
        if (!securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            sanaImage.setSanaScopeIds(securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation());
        }
        if (sanaId != null) {
            assertCanOperateSana(sanaId);
            sanaImage.setSanaId(sanaId);
        }
        return sanaImageQueryService.page(sanaImage);
    }

    /**
     * 方法：assertCanOperateSana
     *
     * @author zhanghongyu
     */
    private void assertCanOperateSana(Long sanaId) {
        if (!securityHelper.canOperateSanaForSensitiveOperation(sanaId)) {
            throw new AccessDeniedException("无权操作其他机构图片数据");
        }
    }

    private void requireResource(String resourcePath) {
        if (!securityHelper.hasResourcePathForSensitiveOperation(resourcePath)) {
            throw new AccessDeniedException("无权访问机构图片功能");
        }
    }
}
