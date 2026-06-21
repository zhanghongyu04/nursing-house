package com.zhiling.system.interfaces.system;

import com.zhiling.framework.system.port.MediaFilePort;
import com.zhiling.system.application.service.CommonFileService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体文件公开契约适配器。
 *
 * @author zhanghongyu
 */
@Component
public class MediaFilePortAdapter implements MediaFilePort {

    private final CommonFileService commonFileService;

    /**
     * 构造器：MediaFilePortAdapter
     *
     * @author zhanghongyu
     */
    public MediaFilePortAdapter(CommonFileService commonFileService) {
        this.commonFileService = commonFileService;
    }

    /**
     * 方法：upload
     *
     * @author zhanghongyu
     */
    @Override
    public String upload(MultipartFile file) {
        return commonFileService.upload(file);
    }

    /**
     * 方法：downloadAsBytesByUrl
     *
     * @author zhanghongyu
     */
    @Override
    public byte[] downloadAsBytesByUrl(String fileUrl) {
        return commonFileService.downloadAsBytesByUrl(fileUrl);
    }
}