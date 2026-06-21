package com.zhiling.agent.infrastructure.support;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 基于内存的 MultipartFile 实现，用于重放历史媒体附件。
 *
 * @author zhanghongyu
 */
public class InMemoryMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] content;

    /**
     * 构造器：InMemoryMultipartFile
     *
     * @author zhanghongyu
     */
    public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.content = content == null ? new byte[0] : content;
    }

    /**
     * 方法：getName
     *
     * @author zhanghongyu
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 方法：getOriginalFilename
     *
     * @author zhanghongyu
     */
    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    /**
     * 方法：getContentType
     *
     * @author zhanghongyu
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    /**
     * 方法：isEmpty
     *
     * @author zhanghongyu
     */
    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    /**
     * 方法：getSize
     *
     * @author zhanghongyu
     */
    @Override
    public long getSize() {
        return content.length;
    }

    /**
     * 方法：getBytes
     *
     * @author zhanghongyu
     */
    @Override
    public byte[] getBytes() {
        return content.clone();
    }

    /**
     * 方法：getInputStream
     *
     * @author zhanghongyu
     */
    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        transferTo(dest.toPath());
    }

    @Override
    public void transferTo(Path dest) throws IOException, IllegalStateException {
        Files.write(dest, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}