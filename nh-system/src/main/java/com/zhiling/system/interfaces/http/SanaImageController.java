package com.zhiling.system.interfaces.http;
import com.zhiling.common.result.PageResult;
import com.zhiling.common.result.Result;
import com.zhiling.model.dto.SanaImagePageQueryDto;
import com.zhiling.model.entity.SanaImage;
import com.zhiling.system.sanaimage.service.SanaImageDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/sanaImage")
@Slf4j
@Tag(name = "养老院图片管理", description = "养老院图片管理相关接口")
/**
 * SanaImageController
 *
 * @author zhanghongyu
 */
public class SanaImageController {

    private final SanaImageDomainService sanaImageDomainService;

    /**
     * 构造器：SanaImageController
     *
     * @author zhanghongyu
     */
    public SanaImageController(SanaImageDomainService sanaImageDomainService) {
        this.sanaImageDomainService = sanaImageDomainService;
    }

    /**
     * 方法：add
     *
     * @author zhanghongyu
     */
    @PostMapping("/add")
    @Operation(summary = "新增养老院图片信息")
    public Result<Boolean> add(@RequestBody SanaImage sanaImage) {
        return Result.success(sanaImageDomainService.save(sanaImage));
    }

    /**
     * 方法：delete
     *
     * @author zhanghongyu
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除养老院图片信息")
    public Result<Boolean> delete(@RequestBody SanaImage sanaImage) {
        return Result.success(sanaImageDomainService.remove(sanaImage));
    }

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询养老院图片信息")
    public Result<PageResult> page(@RequestBody SanaImagePageQueryDto sanaImage) {
        return Result.success(sanaImageDomainService.page(sanaImage));
    }


}


