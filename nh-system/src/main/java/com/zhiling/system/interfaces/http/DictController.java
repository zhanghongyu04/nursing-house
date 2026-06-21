package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.Result;
import com.zhiling.model.vo.DictItemVo;
import com.zhiling.system.dict.service.DictDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dict")
@Tag(name = "统一字典", description = "统一字典查询接口")
/**
 * DictController
 *
 * @author zhanghongyu
 */
public class DictController {

    private final DictDomainService dictDomainService;

    /**
     * 构造器：DictController
     *
     * @author zhanghongyu
     */
    public DictController(DictDomainService dictDomainService) {
        this.dictDomainService = dictDomainService;
    }

    /**
     * 方法：RequestParam
     *
     * @author zhanghongyu
     */
    @GetMapping("/items")
    @Operation(summary = "查询单个字典类型的字典项")
    public Result<List<DictItemVo>> listItems(@RequestParam("dictTypeCode") String dictTypeCode) {
        return Result.success(dictDomainService.listByDictTypeCode(dictTypeCode));
    }

    /**
     * 方法：RequestParam
     *
     * @author zhanghongyu
     */
    @GetMapping("/items/batch")
    @Operation(summary = "批量查询字典项（按字典类型分组）")
    public Result<Map<String, List<DictItemVo>>> listItemsBatch(@RequestParam("dictTypeCodes") String dictTypeCodes) {
        List<String> codes = dictTypeCodes == null ? List.of() : List.of(dictTypeCodes.split(","));
        return Result.success(dictDomainService.listByDictTypeCodes(codes));
    }
}

