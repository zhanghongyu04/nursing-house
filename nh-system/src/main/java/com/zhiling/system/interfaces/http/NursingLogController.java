package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.PageResult;
import com.zhiling.common.result.Result;
import com.zhiling.model.dto.NursingLogAddDto;
import com.zhiling.model.dto.NursingLogExportDto;
import com.zhiling.model.dto.NursingLogMyPageQueryDto;
import com.zhiling.model.dto.NursingLogPageQueryDto;
import com.zhiling.model.dto.NursingLogUpdateDto;
import com.zhiling.system.application.service.NursingLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 护理日志控制器
 *
 * @author zhanghongyu
 */
@RestController
@RequestMapping("/api/v1/nursing-log")
@Tag(name = "护理日志管理", description = "护理日志管理相关接口")
public class NursingLogController {

    private final NursingLogService nursingLogService;

    /**
     * 构造器：NursingLogController
     *
     * @author zhanghongyu
     */
    public NursingLogController(NursingLogService nursingLogService) {
        this.nursingLogService = nursingLogService;
    }

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @PostMapping("/page")
    @Operation(summary = "机构侧护理日志分页")
    public Result<PageResult> page(@RequestBody NursingLogPageQueryDto queryDto) {
        return Result.success(nursingLogService.page(queryDto));
    }

    /**
     * 方法：myPage
     *
     * @author zhanghongyu
     */
    @PostMapping("/my/page")
    @Operation(summary = "护理端我的日志分页")
    public Result<PageResult> myPage(@RequestBody NursingLogMyPageQueryDto queryDto) {
        return Result.success(nursingLogService.myPage(queryDto));
    }

    /**
     * 方法：add
     *
     * @author zhanghongyu
     */
    @PostMapping("/add")
    @Operation(summary = "护理端新增日志")
    public Result<Boolean> add(@RequestBody NursingLogAddDto addDto) {
        return Result.success(nursingLogService.add(addDto));
    }

    /**
     * 方法：PathVariable
     *
     * @author zhanghongyu
     */
    @PutMapping("/update/{id}")
    @Operation(summary = "护理端更新本人日志")
    public Result<Boolean> update(@PathVariable("id") Long id, @RequestBody NursingLogUpdateDto updateDto) {
        return Result.success(nursingLogService.update(id, updateDto));
    }

    /**
     * 方法：RequestBody
     *
     * @author zhanghongyu
     */
    @PostMapping("/export")
    @Operation(summary = "机构侧导出护理日志（ZIP）")
    public ResponseEntity<byte[]> export(@RequestBody(required = false) NursingLogExportDto exportDto) {
        byte[] bytes = nursingLogService.export(exportDto);
        String fileName = "nursing-log-export-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ".zip";
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedName)
                .body(bytes);
    }
}