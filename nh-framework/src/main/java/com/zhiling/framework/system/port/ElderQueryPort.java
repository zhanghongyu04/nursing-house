package com.zhiling.framework.system.port;

import com.zhiling.framework.system.model.ElderCareLevelStats;

/**
 * 老人统计查询公开契约。
 *
 * @author zhanghongyu
 */
public interface ElderQueryPort {

    ElderCareLevelStats getCareLevelStats(String sanaName);

    java.util.List<ElderCareLevelStats> listCurrentScopeCareLevelStats();
}