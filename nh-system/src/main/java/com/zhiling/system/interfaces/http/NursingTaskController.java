package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.PageResult;
import com.zhiling.common.result.Result;
import com.zhiling.model.dto.NursingTaskDispatchDto;
import com.zhiling.model.dto.NursingTaskMyPageQueryDto;
import com.zhiling.model.dto.NursingTaskPageQueryDto;
import com.zhiling.model.dto.NursingTaskUpdateDto;
import com.zhiling.system.application.service.NursingTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 护理任务控制器
 *
 * @author zhanghongyu
 */
@RestController
@RequestMapping("/api/v1/nursing-task")
@Tag(name = "护理任务管理", description = "护理任务管理相关接口")
public class NursingTaskController {

    private final NursingTaskService nursingTaskService;

    /**
     * 构造器：NursingTaskController
     *
     * @author zhanghongyu
     */
    public NursingTaskController(NursingTaskService nursingTaskService) {
        this.nursingTaskService = nursingTaskService;
    }

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @PostMapping("/page")
    @Operation(summary = "机构侧护理任务分页")
    public Result<PageResult> page(@RequestBody NursingTaskPageQueryDto queryDto) {
        return Result.success(nursingTaskService.page(queryDto));
    }

    /**
     * 方法：dispatch
     *
     * @author zhanghongyu
     */
    @PostMapping("/dispatch")
    @Operation(summary = "下发护理任务")
    public Result<Boolean> dispatch(@RequestBody NursingTaskDispatchDto dispatchDto) {
        return Result.success(nursingTaskService.dispatch(dispatchDto));
    }

    /**
     * 方法：update
     *
     * @author zhanghongyu
     */
    @PutMapping("/update")
    @Operation(summary = "更新护理任务")
    public Result<Boolean> update(@RequestBody NursingTaskUpdateDto updateDto) {
        return Result.success(nursingTaskService.update(updateDto));
    }

    /**
     * 方法：PathVariable
     *
     * @author zhanghongyu
     */
    @PostMapping("/cancel/{id}")
    @Operation(summary = "取消护理任务")
    public Result<Boolean> cancel(@PathVariable("id") Long id) {
        return Result.success(nursingTaskService.cancel(id));
    }

    /**
     * 方法：PathVariable
     *
     * @author zhanghongyu
     */
    @PostMapping("/reactivate/{id}")
    @Operation(summary = "重新激活护理任务")
    public Result<Boolean> reactivate(@PathVariable("id") Long id) {
        return Result.success(nursingTaskService.reactivate(id));
    }

    /**
     * 方法：PathVariable
     *
     * @author zhanghongyu
     */
    @DeleteMapping("/remove/{id}")
    @Operation(summary = "软删除护理任务")
    public Result<Boolean> remove(@PathVariable("id") Long id) {
        return Result.success(nursingTaskService.remove(id));
    }

    /**
     * 方法：myPage
     *
     * @author zhanghongyu
     */
    @PostMapping("/my/page")
    @Operation(summary = "护理端我的任务分页")
    public Result<PageResult> myPage(@RequestBody NursingTaskMyPageQueryDto queryDto) {
        return Result.success(nursingTaskService.myPage(queryDto));
    }

    /**
     * 方法：PathVariable
     *
     * @author zhanghongyu
     */
    @PostMapping("/complete/{id}")
    @Operation(summary = "护理端完成任务")
    public Result<Boolean> complete(@PathVariable("id") Long id) {
        return Result.success(nursingTaskService.complete(id));
    }
}