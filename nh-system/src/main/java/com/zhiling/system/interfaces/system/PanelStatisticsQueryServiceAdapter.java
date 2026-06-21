package com.zhiling.system.interfaces.system;

import com.zhiling.model.entity.Sanatorium;
import com.zhiling.model.vo.RegionSanaCountVo;
import com.zhiling.system.panel.service.PanelStatisticsQueryService;
import com.zhiling.system.infrastructure.persistence.query.ElderQueryService;
import com.zhiling.system.infrastructure.persistence.query.SanatoriumQueryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 大屏统计查询适配器。
 *
 * 适配领域服务接口，委托持久化查询服务执行查询。
 *
 * @author zhanghongyu
 */
@Component
public class PanelStatisticsQueryServiceAdapter implements PanelStatisticsQueryService {

    private final ElderQueryService elderQueryService;
    private final SanatoriumQueryService sanatoriumQueryService;

    public PanelStatisticsQueryServiceAdapter(ElderQueryService elderQueryService,
                                               SanatoriumQueryService sanatoriumQueryService) {
        this.elderQueryService = elderQueryService;
        this.sanatoriumQueryService = sanatoriumQueryService;
    }

    /**
     * 方法：elderCount
     *
     * @author zhanghongyu
     */
    @Override
    public Integer elderCount() {
        return elderQueryService.count();
    }

    /**
     * 方法：elderCountBySanaIds
     *
     * @author zhanghongyu
     */
    @Override
    public Integer elderCountBySanaIds(Set<Long> sanaIds) {
        return elderQueryService.countBySanaIds(sanaIds);
    }

    /**
     * 方法：sanatoriumCount
     *
     * @author zhanghongyu
     */
    @Override
    public Integer sanatoriumCount() {
        return sanatoriumQueryService.count();
    }

    /**
     * 方法：nurseCount
     *
     * @author zhanghongyu
     */
    @Override
    public Integer nurseCount() {
        return sanatoriumQueryService.countNurse();
    }

    /**
     * 方法：medicineCount
     *
     * @author zhanghongyu
     */
    @Override
    public Integer medicineCount() {
        return sanatoriumQueryService.countMedicine();
    }

    /**
     * 方法：affiliationCount
     *
     * @author zhanghongyu
     */
    @Override
    public Integer affiliationCount() {
        return sanatoriumQueryService.countAffiliation();
    }

    /**
     * 方法：useBedCount
     *
     * @author zhanghongyu
     */
    @Override
    public Integer useBedCount() {
        return sanatoriumQueryService.countBedInUse();
    }

    /**
     * 方法：bedCount
     *
     * @author zhanghongyu
     */
    @Override
    public Integer bedCount() {
        return sanatoriumQueryService.countBed();
    }

    /**
     * 方法：listSanatoriumByIds
     *
     * @author zhanghongyu
     */
    @Override
    public List<Sanatorium> listSanatoriumByIds(Set<Long> sanaIds) {
        return sanatoriumQueryService.listByIds(sanaIds);
    }

    /**
     * 方法：regionSanaCount
     *
     * @author zhanghongyu
     */
    @Override
    public List<RegionSanaCountVo> regionSanaCount() {
        return sanatoriumQueryService.listRegionSanaCount();
    }
}
