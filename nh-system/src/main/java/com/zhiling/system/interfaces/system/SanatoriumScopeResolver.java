package com.zhiling.system.interfaces.system;

import cn.hutool.core.util.StrUtil;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.model.entity.Sanatorium;
import com.zhiling.system.infrastructure.persistence.query.SanatoriumQueryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 机构范围统一解析器。
 *
 * 封装“政府管理员可全局访问、普通用户受机构范围约束”的解析策略，
 * 供 Port Adapter 复用，避免每个 adapter 各自维护重复的范围判断逻辑。
 *
 * @author zhanghongyu
 */
@Component
public class SanatoriumScopeResolver {

    private final SanatoriumQueryService sanatoriumQueryService;
    private final SecurityHelper securityHelper;

    public SanatoriumScopeResolver(SanatoriumQueryService sanatoriumQueryService,
                                   SecurityHelper securityHelper) {
        this.sanatoriumQueryService = sanatoriumQueryService;
        this.securityHelper = securityHelper;
    }

    /**
     * 当前用户是否为政府管理员。
     */
    public boolean isGovAdmin() {
        return securityHelper.hasGovAdminRoleForSensitiveOperation();
    }

    /**
     * 获取当前用户的机构范围 IDs。
     */
    public Set<Long> currentScopeIds() {
        return securityHelper.getCurrentSanaScopeIdsForSensitiveOperation();
    }

    /**
     * 解析政府管理员可访问的指定机构（模糊匹配）。
     *
     * @param sanaName 机构名称
     * @return 匹配到的机构，未匹配返回 null
     */
    public Sanatorium resolveGovAccessibleSanatorium(String sanaName) {
        Sanatorium exact = sanatoriumQueryService.selectByExactName(sanaName);
        if (exact != null) {
            return exact;
        }
        List<Sanatorium> candidates = sanatoriumQueryService.searchByKeyword(sanaName, 2, null);
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    /**
     * 在当前用户机构范围内解析指定机构。
     *
     * 若 sanaName 为空，则优先使用当前机构 ID，或取唯一范围机构。
     *
     * @param sanaName 机构名称（可为空）
     * @return 范围内匹配的机构，未匹配返回 null
     */
    public Sanatorium resolveScopedSanatorium(String sanaName) {
        Set<Long> sanaScopeIds = currentScopeIds();
        if (sanaScopeIds.isEmpty()) {
            return null;
        }
        if (StrUtil.isBlank(sanaName)) {
            Long currentSanaId = securityHelper.getCurrentSanaId();
            if (currentSanaId != null && sanaScopeIds.contains(currentSanaId)) {
                return sanatoriumQueryService.selectById(currentSanaId);
            }
            if (sanaScopeIds.size() == 1) {
                return sanatoriumQueryService.selectById(sanaScopeIds.iterator().next());
            }
            return null;
        }
        Long targetSanaId = sanatoriumQueryService.selectIdByName(sanaName);
        if (targetSanaId == null || !sanaScopeIds.contains(targetSanaId)) {
            return null;
        }
        return sanatoriumQueryService.selectById(targetSanaId);
    }

    /**
     * 获取当前用户机构范围内的所有机构（按名称排序）。
     */
    public List<Sanatorium> resolveScopedSanatoriums() {
        Set<Long> sanaScopeIds = currentScopeIds();
        if (sanaScopeIds.isEmpty()) {
            return List.of();
        }
        return sanatoriumQueryService.listByIds(sanaScopeIds).stream()
                .filter(Objects::nonNull)
                .sorted((left, right) -> StrUtil.compare(left.getSanaName(), right.getSanaName(), true))
                .toList();
    }

    /**
     * 解析当前用户的范围名称（用于展示）。
     */
    public String resolveScopeName() {
        Set<Long> sanaScopeIds = currentScopeIds();
        if (sanaScopeIds.isEmpty()) {
            return "当前机构";
        }
        if (sanaScopeIds.size() == 1) {
            Sanatorium sanatorium = sanatoriumQueryService.selectById(sanaScopeIds.iterator().next());
            return sanatorium == null ? "当前机构" : sanatorium.getSanaName();
        }
        return "多机构范围";
    }
}