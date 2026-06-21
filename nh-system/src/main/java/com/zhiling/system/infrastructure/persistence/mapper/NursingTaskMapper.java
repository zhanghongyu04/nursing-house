package com.zhiling.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.NursingTaskMyPageQueryDto;
import com.zhiling.model.dto.NursingTaskPageQueryDto;
import com.zhiling.model.entity.NursingTask;
import com.zhiling.system.infrastructure.persistence.provider.NursingTaskSqlProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 护理任务Mapper
 *
 * @author zhanghongyu
 */
@Mapper
public interface NursingTaskMapper extends BaseMapper<NursingTask> {

    /**
     * 机构侧任务分页
     */
    @SelectProvider(type = NursingTaskSqlProvider.class, method = "page")
    IPage<NursingTask> page(Page<NursingTask> page, @Param("dto") NursingTaskPageQueryDto dto);

    /**
     * 护理端我的任务分页
     */
    @SelectProvider(type = NursingTaskSqlProvider.class, method = "myPage")
    IPage<NursingTask> myPage(Page<NursingTask> page, @Param("dto") NursingTaskMyPageQueryDto dto);

    /**
     * 将到达计划开始时间的待执行任务批量标记为执行中。
     */
    @Update("""
            <script>
            UPDATE tb_nursing_task
            SET status = #{runningStatus},
                update_time = NOW()
            WHERE status = 0
              AND deleted = 0
              AND completion_time IS NULL
              AND planned_start_time IS NOT NULL
              AND planned_start_time <![CDATA[<=]]> #{now}
              AND (planned_end_time IS NULL OR planned_end_time <![CDATA[>]]> #{now})
            </script>
            """)
    int markRunningTasks(@Param("now") LocalDateTime now, @Param("runningStatus") Integer runningStatus);

    /**
     * 将已超出计划结束时间且未完成的任务批量标记为超时。
     */
    @Update("""
            <script>
            UPDATE tb_nursing_task
            SET status = #{overdueStatus},
                completion_time = NULL,
                update_time = NOW()
            WHERE status IN (0, 1)
              AND deleted = 0
              AND completion_time IS NULL
              AND planned_end_time IS NOT NULL
              AND planned_end_time <![CDATA[<=]]> #{now}
            </script>
            """)
    int markOverdueTasks(@Param("now") LocalDateTime now, @Param("overdueStatus") Integer overdueStatus);
}
