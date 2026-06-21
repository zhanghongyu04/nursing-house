package com.zhiling.framework.system.port;

import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体文件访问公开契约。
 *
 * @author zhanghongyu
 */
public interface MediaFilePort {

    /**
     * 上传媒体文件并返回可访问 URL。
     *
     * @param file 媒体文件
     * @return 文件 URL
     */
    String upload(MultipartFile file);

    /**
     * 根据已存储的文件 URL 下载媒体字节。
     *
     * @param fileUrl 文件访问 URL
     * @return 文件字节数组，失败返回 null
     */
    byte[] downloadAsBytesByUrl(String fileUrl);
}