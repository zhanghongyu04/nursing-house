package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.Result;
import com.zhiling.framework.llm.model.VectorStoreDocumentInfo;
import com.zhiling.framework.llm.model.VectorStoreDocumentListResult;
import com.zhiling.framework.llm.model.VectorStoreStats;
import com.zhiling.framework.llm.model.VectorStoreSupportedFileTypes;
import com.zhiling.framework.llm.service.VectorStoreManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vector-store")
@Tag(name = "知识库管理接口")
@Slf4j
/**
 * VectorStoreController
 *
 * @author zhanghongyu
 */
public class VectorStoreController {
    private final VectorStoreManageService vectorStoreManageService;

    /**
     * 构造器：VectorStoreController
     *
     * @author zhanghongyu
     */
    public VectorStoreController(VectorStoreManageService vectorStoreManageService) {
        this.vectorStoreManageService = vectorStoreManageService;
    }

    /**
     * 方法：getStats
     *
     * @author zhanghongyu
     */
    @GetMapping("/stats")
    @Operation(summary = "获取知识库统计信息")
    public Result<VectorStoreStats> getStats() {
        return Result.success(vectorStoreManageService.getStats());
    }

    @GetMapping("/documents")
    @Operation(summary = "获取文档列表")
    public Result<VectorStoreDocumentListResult> getDocuments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(vectorStoreManageService.getDocuments(page, pageSize));
    }

    /**
     * 方法：getDocumentsByFileName
     *
     * @author zhanghongyu
     */
    @GetMapping("/documents/by-filename")
    @Operation(summary = "根据文件名获取文档")
    public Result<List<VectorStoreDocumentInfo>> getDocumentsByFileName(@RequestParam String fileName) {
        return Result.success(vectorStoreManageService.getDocumentsByFileName(fileName));
    }

    /**
     * 方法：deleteDocumentsByFileName
     *
     * @author zhanghongyu
     */
    @DeleteMapping("/documents/by-filename")
    @Operation(summary = "删除指定文件的所有文档")
    public Result<Integer> deleteDocumentsByFileName(@RequestParam String fileName) {
        return Result.success(vectorStoreManageService.deleteDocumentsByFileName(fileName));
    }

    /**
     * 方法：deleteDocument
     *
     * @author zhanghongyu
     */
    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "删除指定文档")
    public Result<Boolean> deleteDocument(@PathVariable String documentId) {
        return Result.success(vectorStoreManageService.deleteDocument(documentId));
    }

    /**
     * 方法：deleteDocuments
     *
     * @author zhanghongyu
     */
    @DeleteMapping("/documents/batch")
    @Operation(summary = "批量删除文档")
    public Result<Integer> deleteDocuments(@RequestBody List<String> documentIds) {
        return Result.success(vectorStoreManageService.deleteDocuments(documentIds));
    }

    /**
     * 方法：clearAll
     *
     * @author zhanghongyu
     */
    @DeleteMapping("/clear")
    @Operation(summary = "清空知识库")
    public Result<Integer> clearAll() {
        return Result.success(vectorStoreManageService.clearAll());
    }

    @PostMapping("/upload")
    @Operation(summary = "上传单个文档到知识库")
    public Result<String> uploadDocument(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "sanaId", required = false) Long sanaId) {
        try {
            return Result.success(vectorStoreManageService.uploadDocument(file, sanaId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文档到知识库")
    public Result<String> uploadDocuments(@RequestParam("files") List<MultipartFile> files,
                                          @RequestParam(value = "sanaId", required = false) Long sanaId) {
        try {
            return Result.success(vectorStoreManageService.uploadDocuments(files, sanaId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 方法：getSupportedFileTypes
     *
     * @author zhanghongyu
     */
    @GetMapping("/supported-types")
    @Operation(summary = "获取支持的文件类型")
    public Result<VectorStoreSupportedFileTypes> getSupportedFileTypes() {
        return Result.success(vectorStoreManageService.getSupportedFileTypes());
    }
}

