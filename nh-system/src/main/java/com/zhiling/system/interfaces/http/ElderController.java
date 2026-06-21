package com.zhiling.system.interfaces.http;
import com.zhiling.common.result.PageResult;
import com.zhiling.common.result.Result;
import com.zhiling.common.utils.ExcelUtil;
import com.zhiling.model.dto.ElderDto;
import com.zhiling.model.dto.ElderExcelDto;
import com.zhiling.model.dto.ElderImportExcelDto;
import com.zhiling.model.dto.ElderPageQueryDto;
import com.zhiling.model.entity.Elder;
import com.zhiling.system.application.service.ElderService;
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

@RestController
@RequestMapping("/api/v1/elder")
/**
 * 老人信息管理控制器
 *
 * @author zhanghongyu
 */
@Tag(name = "老人信息管理", description = "老人信息管理")
public class ElderController {
    private static final Logger log = LoggerFactory.getLogger(ElderController.class);
    private final ElderService elderService;

    /**
     * 构造器：ElderController
     *
     * @author zhanghongyu
     */
    public ElderController(ElderService elderService) {
        this.elderService = elderService;
    }

    /**
     * 方法：add
     *
     * @author zhanghongyu
     */
    @PostMapping("/add")
    @Operation(summary = "添加老人信息")
    public Result<Boolean> add(@RequestBody Elder elder) {
        return Result.success(elderService.add(elder));
    }

    /**
     * 方法：RequestParam
     *
     * @author zhanghongyu
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除老人信息")
    public Result<Boolean> delete(@RequestParam("id") Long id) {
        return Result.success(elderService.delete(id));
    }

    /**
     * 方法：update
     *
     * @author zhanghongyu
     */
    @PutMapping("/update")
    @Operation(summary = "修改老人信息")
    public Result<Boolean> update(@RequestBody ElderDto elder) {
        return Result.success(elderService.update(elder));
    }

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询老人信息")
    public Result<PageResult> page(@RequestBody ElderPageQueryDto elderPageQueryDto) {
        return Result.success(elderService.page(elderPageQueryDto));
    }



     /**
      * 导出老人信息Excel
      * @param response
      */
     @PostMapping("/export")
     @Operation(summary = "导出老人信息Excel")
     public void exportExcel(@RequestBody(required = false) ElderPageQueryDto queryDto, HttpServletResponse response) {
         log.info("开始导出老人信息Excel");

         try {
             List<ElderExcelDto> list = elderService.listForExport(queryDto);

             if (list == null || list.isEmpty()) {
                 response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                 response.getWriter().write("暂无数据可导出");
                 return;
             }

             ExcelUtil.excelExport(response, "老人信息列表", "老人信息", ElderExcelDto.class, list);

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
     * 导入老人信息Excel
     * @param file
     * @return
     */
    @PostMapping("/import")
    @Operation(summary = "导入老人信息Excel")
    public Result<?> importExcel(@RequestParam("file") MultipartFile file) {
        log.info("开始导入老人信息Excel");

        try {
            if (file == null || file.isEmpty()) {
                return Result.fail("导入文件不能为空");
            }

            List<ElderImportExcelDto> list = ExcelUtil.readSimple(file, ElderImportExcelDto.class);
            if (list == null || list.isEmpty()) {
                return Result.fail("导入数据为空");
            }
            log.info("Excel读取成功，共{}条数据，开始执行数据库插入", list.size());

            ElderService.ImportResult result = elderService.importElders(list);

            String resultMsg = String.format(
                    "导入完成：共读取%s条数据，插入成功%s条，插入失败%s条",
                    list.size(), result.getSuccessCount(), result.getFailCount()
            );
            if (!result.getFailDetails().isEmpty()) {
                int showLimit = 50;
                String failSummary = result.getFailDetails().size() <= showLimit
                        ? String.join("；", result.getFailDetails())
                        : String.join("；", result.getFailDetails().subList(0, showLimit)) + "（共" + result.getFailDetails().size() + "条失败，剩余略）";
                resultMsg += "；失败详情：" + failSummary;
            }

            return Result.success(resultMsg);

        } catch (Exception e) {
            log.error("Excel导入全局异常：", e);
            return Result.fail("导入失败（全局错误）：" + e.getMessage());
        }
    }
}
