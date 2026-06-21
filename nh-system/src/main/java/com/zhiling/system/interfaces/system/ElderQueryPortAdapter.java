package com.zhiling.system.interfaces.system;

import cn.hutool.core.util.StrUtil;
import com.zhiling.framework.system.model.ElderCareLevelStats;
import com.zhiling.framework.system.port.ElderQueryPort;
import com.zhiling.model.entity.Sanatorium;
import com.zhiling.system.infrastructure.persistence.query.ElderQueryService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 老人统计查询公开契约适配器。
 *
 * 适配 port 接口，委托持久化查询服务执行查询。
 *
 * @author zhanghongyu
 */
@Component
public class ElderQueryPortAdapter implements ElderQueryPort {

    private final ElderQueryService elderQueryService;
    private final SanatoriumScopeResolver scopeResolver;

    public ElderQueryPortAdapter(ElderQueryService elderQueryService,
                                 SanatoriumScopeResolver scopeResolver) {
        this.elderQueryService = elderQueryService;
        this.scopeResolver = scopeResolver;
    }

    /**
     * 方法：getCareLevelStats
     *
     * @author zhanghongyu
     */
    @Override
    public ElderCareLevelStats getCareLevelStats(String sanaName) {
        if (scopeResolver.isGovAdmin()) {
            if (StrUtil.isBlank(sanaName)) {
                return ElderCareLevelStats.builder()
                        .scope("global")
                        .scopeName("全局")
                        .distribution(formatDistribution(elderQueryService.queryAllElderDistribution()))
                        .build();
            }
            Sanatorium sanatorium = scopeResolver.resolveGovAccessibleSanatorium(sanaName);
            if (sanatorium == null) {
                return null;
            }
            return ElderCareLevelStats.builder()
                    .scope("organization")
                    .scopeName(sanatorium.getSanaName())
                    .distribution(formatDistribution(elderQueryService.queryElderDistributionBySanaId(sanatorium.getId())))
                    .build();
        }

        Sanatorium scoped = scopeResolver.resolveScopedSanatorium(sanaName);
        if (scoped == null) {
            return null;
        }
        return ElderCareLevelStats.builder()
                .scope("organization")
                .scopeName(scoped.getSanaName())
                .distribution(formatDistribution(elderQueryService.queryElderDistributionBySanaId(scoped.getId())))
                .build();
    }

    /**
     * 方法：listCurrentScopeCareLevelStats
     *
     * @author zhanghongyu
     */
    @Override
    public List<ElderCareLevelStats> listCurrentScopeCareLevelStats() {
        if (scopeResolver.isGovAdmin()) {
            return List.of();
        }
        return scopeResolver.resolveScopedSanatoriums().stream()
                .map(item -> ElderCareLevelStats.builder()
                        .scope("organization")
                        .scopeName(item.getSanaName())
                        .distribution(formatDistribution(elderQueryService.queryElderDistributionBySanaId(item.getId())))
                        .build())
                .toList();
    }

    /**
     * 方法：formatDistribution
     *
     * @author zhanghongyu
     */
    private Map<String, Integer> formatDistribution(List<Map<String, Object>> rows) {
        LinkedHashMap<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("能力完好", 0);
        distribution.put("轻度失能", 0);
        distribution.put("中度失能", 0);
        distribution.put("重度失能", 0);
        distribution.put("完全失能", 0);

        for (Map<String, Object> row : rows) {
            Object careType = row.get("self_care");
            Object count = row.get("count");
            distribution.put(formatSelfCare(careType), count instanceof Number ? ((Number) count).intValue() : 0);
        }
        return distribution;
    }

    /**
     * 方法：formatSelfCare
     *
     * @author zhanghongyu
     */
    private String formatSelfCare(Object selfCare) {
        int type = selfCare instanceof Number ? ((Number) selfCare).intValue() : -1;
        return switch (type) {
            case 0 -> "能力完好";
            case 1 -> "轻度失能";
            case 2 -> "中度失能";
            case 3 -> "重度失能";
            case 4 -> "完全失能";
            default -> "未知";
        };
    }
}