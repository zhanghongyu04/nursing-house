package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

 /**
 * 视频监控告警信息实体类
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("tb_video_alert")
public class VideoAlert extends BaseEntity {
    /**
     * 所属养老院ID
     */
    @TableField("sana_id")
    private Long sanaId;

    /**
     * 所属养老院名称（非数据库字段）
     */
    @TableField(exist = false)
    private String sanaName;

    /**
     * 关联设备ID
     */
    @TableField("camera_id")
    private Long cameraId;

    /**
     * 关联设备名称（非数据库字段）
     */
    @TableField(exist = false)
    private String cameraName;

    /**
     * 关联设备位置（非数据库字段）
     */
    @TableField(exist = false)
    private String cameraLocation;

    /**
     * 告警内容（摔倒、滞留、异常行为等）
     */
    @TableField("content")
    private String content;

    /**
     * 告警时间
     */
    @TableField("alert_time")
    private LocalDateTime alertTime;

    /**
     * 处理人
     */
    @TableField("handled_by")
    private Long handledBy;

    /**
     * 处理时间
     */
    @TableField("handled_time")
    private LocalDateTime handledTime;

    /**
     * 处理备注
     */
    @TableField("handle_remark")
    private String handleRemark;

    /**
     * 重写remark字段，标记为非数据库字段（tb_video_alert表没有remark字段）
     */
    @TableField(exist = false)
    private String remark;

    /**
     * 重写create_by字段，标记为非数据库字段（tb_video_alert表没有create_by字段）
     */
    @TableField(exist = false)
    private Long createBy;

    /**
     * 重写update_by字段，标记为非数据库字段（tb_video_alert表没有update_by字段）
     */
    @TableField(exist = false)
    private Long updateBy;
}