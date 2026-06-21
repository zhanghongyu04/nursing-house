package com.zhiling.framework.system.port;

import com.zhiling.framework.system.model.SanatoriumDetailStats;
import com.zhiling.framework.system.model.SanatoriumSummary;

import java.util.List;

/**
 * 机构查询公开契约。
 *
 * @author zhanghongyu
 */
public interface SanatoriumQueryPort {

    List<SanatoriumSummary> searchByKeyword(String keyword, Integer limit);

    SanatoriumDetailStats getDetailStats(String sanaName);

    List<SanatoriumDetailStats> listCurrentScopeDetailStats();
}