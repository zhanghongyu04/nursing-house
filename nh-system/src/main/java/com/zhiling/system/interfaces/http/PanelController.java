package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.Result;
import com.zhiling.model.vo.PanelNavInfo;
import com.zhiling.model.vo.RegionSanaCountVo;
import com.zhiling.system.panel.service.PanelDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/panel")
@Tag(name = "大屏信息", description = "大屏信息")
@Slf4j
/**
 * PanelController
 *
 * @author zhanghongyu
 */
public class PanelController {
    private final PanelDomainService panelDomainService;

    /**
     * 构造器：PanelController
     *
     * @author zhanghongyu
     */
    public PanelController(PanelDomainService panelDomainService) {
        this.panelDomainService = panelDomainService;
    }

    /**
     * 方法：getNavDistribution
     *
     * @author zhanghongyu
     */
    @GetMapping("/getNavDistribution")
    @Operation(summary = "获取大屏头部信息")
    public Result<PanelNavInfo> getNavDistribution() {
        return Result.success(panelDomainService.getNavDistribution());
    }

    /**
     * 方法：regionSanaCount
     *
     * @author zhanghongyu
     */
    @GetMapping("/regionSanaCount")
    @Operation(summary = "获取区域养老院数量统计")
    public Result<List<RegionSanaCountVo>> regionSanaCount() {
        return Result.success(panelDomainService.regionSanaCount());
    }

    /**
     * 方法：exportRegionStats
     *
     * @author zhanghongyu
     */
    @GetMapping("/exportRegionStats")
    @Operation(summary = "导出区域统计口径数据")
    public Result<List<RegionSanaCountVo>> exportRegionStats() {
        return Result.success(panelDomainService.exportRegionStats());
    }

}


