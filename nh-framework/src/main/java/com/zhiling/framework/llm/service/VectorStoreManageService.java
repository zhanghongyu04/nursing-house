package com.zhiling.framework.llm.service;

import com.zhiling.framework.llm.model.VectorStoreDocumentInfo;
import com.zhiling.framework.llm.model.VectorStoreDocumentListResult;
import com.zhiling.framework.llm.model.VectorStoreStats;
import com.zhiling.framework.llm.model.VectorStoreSupportedFileTypes;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 向量库管理服务。
 *
 * @author zhanghongyu
 */
public interface VectorStoreManageService {

    VectorStoreStats getStats();

    VectorStoreDocumentListResult getDocuments(int page, int pageSize);

    List<VectorStoreDocumentInfo> getDocumentsByFileName(String fileName);

    int deleteDocumentsByFileName(String fileName);

    boolean deleteDocument(String documentId);

    int deleteDocuments(List<String> documentIds);

    int clearAll();

    String uploadDocument(MultipartFile file, Long sanaId);

    String uploadDocuments(List<MultipartFile> files, Long sanaId);

    VectorStoreSupportedFileTypes getSupportedFileTypes();
}
