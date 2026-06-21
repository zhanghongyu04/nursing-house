package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.Result;
import com.zhiling.model.entity.ElderAttachment;
import com.zhiling.system.application.service.ElderAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 老人档案附件控制器。
 *
 * @author zhanghongyu
 */
@RestController
@RequestMapping("/api/v1/elder/attachments")
@Tag(name = "老人档案附件管理", description = "老人档案图片与附件管理")
public class ElderAttachmentController {

    private final ElderAttachmentService elderAttachmentService;

    public ElderAttachmentController(ElderAttachmentService elderAttachmentService) {
        this.elderAttachmentService = elderAttachmentService;
    }

    @GetMapping
    @Operation(summary = "查询老人档案附件")
    public Result<List<ElderAttachment>> list(@RequestParam("elderId") Long elderId) {
        return Result.success(elderAttachmentService.listByElderId(elderId));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传老人档案图片或附件")
    public Result<ElderAttachment> upload(@RequestParam("elderId") Long elderId,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "attachmentType", required = false) Integer attachmentType,
                                          @RequestParam(value = "remark", required = false) String remark) {
        return Result.success(elderAttachmentService.upload(elderId, file, attachmentType, remark));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除老人档案附件")
    public Result<Boolean> delete(@RequestParam("id") Long id) {
        return Result.success(elderAttachmentService.delete(id));
    }
}
