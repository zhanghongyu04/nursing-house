package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.PageResult;
import com.zhiling.common.result.Result;
import com.zhiling.common.utils.ExcelUtil;
import com.zhiling.model.dto.SanatoriumDetailPageQueryDto;
import com.zhiling.model.dto.SanatoriumImportExcelDto;
import com.zhiling.model.dto.SanatoriumPageQueryDto;
import com.zhiling.model.entity.Sanatorium;
import com.zhiling.system.application.service.SanatoriumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;


@RestController
@RequestMapping("/api/v1/sanatorium")
@Tag(name = "养老院管理", description = "养老院管理相关接口")
/**
 * SanatoriumController
 *
 * @author zhanghongyu
 */
public class SanatoriumController {
    private static final Logger log = LoggerFactory.getLogger(SanatoriumController.class);
    private final SanatoriumService sanatoriumService;

    /**
     * 构造器：SanatoriumController
     *
     * @author zhanghongyu
     */
    public SanatoriumController(SanatoriumService sanatoriumService) {
        this.sanatoriumService = sanatoriumService;
    }

    /**
     * 方法：add
     *
     * @author zhanghongyu
     */
    @PostMapping("/add")
    @Operation(summary = "新增养老院信息")
    public Result<Boolean> add(@RequestBody Sanatorium sanatorium) {
        return Result.success(sanatoriumService.saveBatch(List.of(sanatorium)));
    }

    /**
     * 方法：RequestParam
     *
     * @author zhanghongyu
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除养老院信息")
    public Result<Boolean> delete(@RequestParam("id") Long id) {
        return Result.success(sanatoriumService.removeById(id));
    }

    /**
     * 方法：update
     *
     * @author zhanghongyu
     */
    @PutMapping("/update")
    @Operation(summary = "修改养老院信息")
    public Result<Boolean> update(@RequestBody Sanatorium sanatorium) {
        return Result.success(sanatoriumService.update(sanatorium));
    }

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询养老院信息")
    public Result<PageResult> page(@RequestBody SanatoriumPageQueryDto sanatoriumPageQueryDto) {
        return Result.success(sanatoriumService.page(sanatoriumPageQueryDto));
    }

    /**
     * 方法：exportExcel
     *
     * @author zhanghongyu
     */
    @PostMapping("/export")
    @Operation(summary = "导出养老院信息Excel")
    public void exportExcel(@RequestBody(required = false) SanatoriumPageQueryDto queryDto, HttpServletResponse response) {
        log.info("开始导出养老院信息Excel");
        try {
            List<Sanatorium> list = sanatoriumService.listForExport(queryDto);
            if (list == null || list.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("暂无数据可导出");
                return;
            }
            ExcelUtil.excelExport(response, "养老院信息列表", "养老院信息", Sanatorium.class, list);
            log.info("Excel导出成功，共导出{}条数据", list.size());
        } catch (ClientAbortException e) {
            log.info("用户取消Excel下载");
        } catch (Exception e) {
            log.error("导出Excel时发生异常: {}", e.getMessage(), e);
            try {
                response.reset();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.setCharacterEncoding("utf-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败，系统异常\"}");
            } catch (IOException ex) {
                log.error("设置错误响应时发生异常: {}", ex.getMessage(), ex);
            }
        }
    }

    /**
     * 方法：RequestParam
     *
     * @author zhanghongyu
     */
    private static final int IMPORT_MAX_ROWS = 500;
    private static final Set<String> ALLOWED_EXCEL_EXTENSIONS = Set.of(".xlsx", ".xls");

    @PostMapping("/import")
    @Operation(summary = "导入养老院信息Excel")
    public Result<SanatoriumService.ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        log.info("开始导入养老院信息Excel");
        try {
            if (file == null || file.isEmpty()) {
                return Result.fail("导入文件不能为空");
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || ALLOWED_EXCEL_EXTENSIONS.stream()
                    .noneMatch(ext -> originalFilename.toLowerCase().endsWith(ext))) {
                return Result.fail("仅支持 .xlsx 或 .xls 格式的 Excel 文件");
            }
            List<SanatoriumImportExcelDto> list = ExcelUtil.readSimple(file, SanatoriumImportExcelDto.class);
            if (list == null || list.isEmpty()) {
                return Result.fail("导入数据为空");
            }
            if (list.size() > IMPORT_MAX_ROWS) {
                return Result.fail(String.format("单次导入不能超过%d条数据，当前%d条", IMPORT_MAX_ROWS, list.size()));
            }
            log.info("Excel读取成功，共{}条数据，开始执行导入", list.size());
            SanatoriumService.ImportResult result = sanatoriumService.importSanatoriums(list);
            log.info("导入完成：成功{}条，失败{}条", result.getSuccessCount(), result.getFailCount());
            if (result.getFailCount() > 0 && result.getSuccessCount() == 0) {
                return Result.fail(400, "导入失败：全部数据校验未通过", result);
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("导入Excel时发生异常: {}", e.getMessage(), e);
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    @GetMapping("/import")
    @Operation(summary = "下载导入模板")
    public void downloadImportTemplate(HttpServletResponse response) {
        log.info("下载康养机构导入模板");
        try {
            ExcelUtil.excelExport(response, "康养机构导入模板", "机构信息",
                    SanatoriumImportExcelDto.class, List.of());
        } catch (ClientAbortException e) {
            log.info("用户取消模板下载");
        } catch (Exception e) {
            log.error("下载模板时发生异常: {}", e.getMessage(), e);
            try {
                response.reset();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.setCharacterEncoding("utf-8");
                response.getWriter().write("{\"code\":500,\"message\":\"下载模板失败\"}");
            } catch (IOException ex) {
                log.error("设置错误响应时发生异常: {}", ex.getMessage(), ex);
            }
        }
    }

    /**
     * 方法：RequestParam
     *
     * @author zhanghongyu
     */
    @GetMapping("/elderDistribution")
    @Operation(summary = "不同自理能力老人数量分布")
    public Result<Map<String, Integer>> elderDistribution(@RequestParam("sanaName") String sanaName) {
        return Result.success(sanatoriumService.elderDistribution(sanaName));
    }

    /**
     * 方法：pageSanaElderList
     *
     * @author zhanghongyu
     */
    @PostMapping("/pageSanaElderList")
    @Operation(summary = "养老院分页详情老人信息分页查询")
    public Result<PageResult> pageSanaElderList(@RequestBody SanatoriumDetailPageQueryDto sanatoriumDetailPageQueryDto) {
        return Result.success(sanatoriumService.pageSanaElderList(sanatoriumDetailPageQueryDto));
    }


}
