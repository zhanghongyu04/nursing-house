package com.zhiling.framework.system.port;

import com.zhiling.framework.system.model.NursingLogQueryResult;
import com.zhiling.framework.system.model.NursingTaskQueryResult;

import java.util.Set;

/**
 * 护理任务与护理日志查询公开契约。
 *
 * @author zhanghongyu
 */
public interface NursingCareQueryPort {

    NursingTaskQueryResult listNursingTasks(String sanaName, Set<Integer> statuses, Integer limit);

    NursingLogQueryResult listNursingLogs(String sanaName, Integer abnormalFlag, Integer limit);
}
