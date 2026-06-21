package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.Result;
import com.zhiling.model.dto.TaskTemplateCreateDto;
import com.zhiling.model.dto.TaskTemplatePageQueryDto;
import com.zhiling.system.application.service.NursingTaskTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 护理任务模板管理
 *
 * @author zhanghongyu
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/nursing-task-template")
@RequiredArgsConstructor
@Tag(name = "护理任务模板管理")
public class NursingTaskTemplateController {

    private final NursingTaskTemplateService templateService;

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询任务模板")
    public Result<?> page(@RequestBody TaskTemplatePageQueryDto dto) {
        return Result.success(templateService.page(dto));
    }

    /**
     * 方法：create
     *
     * @author zhanghongyu
     */
    @PostMapping("/create")
    @Operation(summary = "创建任务模板")
    public Result<Boolean> create(@RequestBody TaskTemplateCreateDto dto) {
        return Result.success(templateService.create(dto));
    }

    /**
     * 方法：update
     *
     * @author zhanghongyu
     */
    @PutMapping("/update")
    @Operation(summary = "更新任务模板")
    public Result<Boolean> update(@RequestBody TaskTemplateCreateDto dto) {
        return Result.success(templateService.update(dto));
    }

    /**
     * 方法：toggleEnabled
     *
     * @author zhanghongyu
     */
    @PostMapping("/toggle/{id}")
    @Operation(summary = "启用/禁用任务模板")
    public Result<Boolean> toggleEnabled(@PathVariable Long id) {
        return Result.success(templateService.toggleEnabled(id));
    }

    /**
     * 方法：remove
     *
     * @author zhanghongyu
     */
    @DeleteMapping("/remove/{id}")
    @Operation(summary = "删除任务模板")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(templateService.remove(id));
    }

    /**
     * 方法：generate
     *
     * @author zhanghongyu
     */
    @PostMapping("/generate/{id}")
    @Operation(summary = "手动触发生成任务")
    public Result<Integer> generate(@PathVariable Long id) {
        int count = templateService.generateTasksForTemplate(id);
        return Result.success(count);
    }
}