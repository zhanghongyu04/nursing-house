package com.zhiling.framework.llm.model;

import java.util.List;

/**
 * 向量库支持文件类型。
 *
 * @author zhanghongyu
 */
public record VectorStoreSupportedFileTypes(List<String> fileExtensions, List<String> mimeTypes) {
}