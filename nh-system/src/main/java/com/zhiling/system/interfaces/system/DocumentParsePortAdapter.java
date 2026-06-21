package com.zhiling.system.interfaces.system;

import com.zhiling.framework.system.port.DocumentParsePort;
import com.zhiling.system.application.service.DocumentReaderService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档解析公开契约适配器。
 *
 * @author zhanghongyu
 */
@Component
public class DocumentParsePortAdapter implements DocumentParsePort {

    private final DocumentReaderService documentReaderService;

    /**
     * 构造器：DocumentParsePortAdapter
     *
     * @author zhanghongyu
     */
    public DocumentParsePortAdapter(DocumentReaderService documentReaderService) {
        this.documentReaderService = documentReaderService;
    }

    /**
     * 方法：readDocumentTexts
     *
     * @author zhanghongyu
     */
    @Override
    public List<String> readDocumentTexts(String fileName, byte[] content) {
        return documentReaderService.readDocument(fileName, content).stream()
                .map(document -> document.getText())
                .toList();
    }
}