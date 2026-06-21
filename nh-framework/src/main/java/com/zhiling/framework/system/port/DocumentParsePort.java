package com.zhiling.framework.system.port;

import java.util.List;

/**
 * 文档解析公开契约。
 * 面向跨模块调用，仅暴露稳定文本结果，避免泄漏 system 内部实现类型。
 *
 * @author zhanghongyu
 */
public interface DocumentParsePort {

    /**
     * 解析文档并返回文本块列表。
     *
     * @param fileName 文件名
     * @param content 文件字节内容
     * @return 文本块列表
     */
    List<String> readDocumentTexts(String fileName, byte[] content);
}