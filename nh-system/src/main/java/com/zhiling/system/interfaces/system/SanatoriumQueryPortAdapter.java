package com.zhiling.system.interfaces.system;

import cn.hutool.core.util.StrUtil;
import com.zhiling.framework.system.model.SanatoriumDetailStats;
import com.zhiling.framework.system.model.SanatoriumSummary;
import com.zhiling.framework.system.port.SanatoriumQueryPort;
import com.zhiling.model.entity.Sanatorium;
import com.zhiling.system.infrastructure.persistence.query.SanatoriumQueryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 机构查询公开契约适配器。
 *
 * 适配 port 接口，委托持久化查询服务执行查询。
 *
 * @author zhanghongyu
 */
@Component
public class SanatoriumQueryPortAdapter implements SanatoriumQueryPort {

    private final SanatoriumQueryService sanatoriumQueryService;
    private final SanatoriumScopeResolver scopeResolver;

    public SanatoriumQueryPortAdapter(SanatoriumQueryService sanatoriumQueryService,
                                       SanatoriumScopeResolver scopeResolver) {
        this.sanatoriumQueryService = sanatoriumQueryService;
        this.scopeResolver = scopeResolver;
    }

    /**
     * 方法：searchByKeyword
     *
     * @author zhanghongyu
     */
    @Override
    public List<SanatoriumSummary> searchByKeyword(String keyword, Integer limit) {
        boolean govAdmin = scopeResolver.isGovAdmin();
        Set<Long> scopeIds = govAdmin ? null : scopeResolver.currentScopeIds();
        if (!govAdmin && scopeIds.isEmpty()) {
            return List.of();
        }
        return sanatoriumQueryService.searchByKeyword(keyword, limit, scopeIds).stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * 方法：getDetailStats
     *
     * @author zhanghongyu
     */
    @Override
    public SanatoriumDetailStats getDetailStats(String sanaName) {
        if (scopeResolver.isGovAdmin()) {
            if (StrUtil.isBlank(sanaName)) {
                return null;
            }
            return toDetail(scopeResolver.resolveGovAccessibleSanatorium(sanaName));
        }

        return toDetail(scopeResolver.resolveScopedSanatorium(sanaName));
    }

    /**
     * 方法：listCurrentScopeDetailStats
     *
     * @author zhanghongyu
     */
    @Override
    public List<SanatoriumDetailStats> listCurrentScopeDetailStats() {
        if (scopeResolver.isGovAdmin()) {
            return List.of();
        }
        return scopeResolver.resolveScopedSanatoriums().stream()
                .map(this::toDetail)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 方法：toSummary
     *
     * @author zhanghongyu
     */
    private SanatoriumSummary toSummary(Sanatorium sanatorium) {
        return SanatoriumSummary.builder()
                .id(sanatorium.getId())
                .sanaName(sanatorium.getSanaName())
                .regionName(sanatorium.getSanaAffiliation())
                .address(sanatorium.getSanaAddress())
                .status(sanatorium.getStatus())
                .bedCount(defaultInt(sanatorium.getBedCount()))
                .bedInUse(defaultInt(sanatorium.getBedInUse()))
                .build();
    }

    /**
     * 方法：toDetail
     *
     * @author zhanghongyu
     */
    private SanatoriumDetailStats toDetail(Sanatorium sanatorium) {
        if (sanatorium == null) {
            return null;
        }
        double useRate = buildUseRate(sanatorium.getBedCount(), sanatorium.getBedInUse());

        // 子机构法人/信用代码为空时，从父机构继承
        String uscc = sanatorium.getUscc();
        String legalPersons = sanatorium.getLegalPersons();
        String legalPhone = sanatorium.getLegalPhone();
        if (sanatorium.getParentSanaId() != null
                && StrUtil.isBlank(uscc) && StrUtil.isBlank(legalPersons)) {
            Sanatorium parent = sanatoriumQueryService.selectById(sanatorium.getParentSanaId());
            if (parent != null) {
                if (StrUtil.isBlank(uscc)) uscc = parent.getUscc();
                if (StrUtil.isBlank(legalPersons)) legalPersons = parent.getLegalPersons();
                if (StrUtil.isBlank(legalPhone)) legalPhone = parent.getLegalPhone();
            }
        }

        return SanatoriumDetailStats.builder()
                .id(sanatorium.getId())
                .sanaName(sanatorium.getSanaName())
                .regionName(sanatorium.getSanaAffiliation())
                .address(sanatorium.getSanaAddress())
                .status(sanatorium.getStatus())
                .elderCount(defaultInt(sanatorium.getElderCount()))
                .nurseCount(defaultInt(sanatorium.getNursingCount()))
                .medicineCount(defaultInt(sanatorium.getMedicalCount()))
                .bedCount(defaultInt(sanatorium.getBedCount()))
                .bedInUse(defaultInt(sanatorium.getBedInUse()))
                .bedUseRate(useRate)
                .bedUseRatePercent(String.format("%.2f%%", useRate * 100))
                .uscc(uscc)
                .legalPersons(legalPersons)
                .legalPhone(legalPhone)
                .build();
    }

    /**
     * 方法：defaultInt
     *
     * @author zhanghongyu
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 方法：buildUseRate
     *
     * @author zhanghongyu
     */
    private double buildUseRate(Integer bedCount, Integer bedInUse) {
        int total = defaultInt(bedCount);
        int used = defaultInt(bedInUse);
        return total == 0 ? 0D : (double) used / total;
    }
}