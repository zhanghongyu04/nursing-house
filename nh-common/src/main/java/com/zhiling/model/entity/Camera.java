package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
 /**
 * 视频监控设备实体类
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("tb_camera")
public class Camera extends BaseEntity {
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
     * 设备名称
     */
    @TableField("camera_name")
    private String cameraName;

    /**
     * 安装位置
     */
    @TableField("camera_location")
    private String cameraLocation;

    /**
     * 设备状态(0-正常，1-故障，2-离线)
     */
    @TableField("camera_status")
    private Integer cameraStatus;

    /**
     * 萤石设备序列号
     */
    @TableField("device_serial")
    private String deviceSerial;

    /**
     * 通道号
     */
    @TableField("channel_no")
    private Integer channelNo;

    /**
     * 设备验证码
     */
    @TableField("validate_code")
    private String validateCode;

    /**
     * 码流质量
     */
    @TableField("stream_quality")
    private String streamQuality;

    /**
     * 平台在线状态
     */
    @TableField("online_status")
    private Integer onlineStatus;

    /**
     * 重写status字段，标记为非数据库字段（tb_camera表没有status字段）
     */
    @TableField(exist = false)
    private Integer status;

    /**
     * 重写remark字段，标记为非数据库字段（tb_camera表没有remark字段）
     */
    @TableField(exist = false)
    private String remark;

    /**
     * 重写create_by字段，标记为非数据库字段（tb_camera表没有create_by字段）
     */
    @TableField(exist = false)
    private Long createBy;

    /**
     * 重写update_by字段，标记为非数据库字段（tb_camera表没有update_by字段）
     */
    @TableField(exist = false)
    private Long updateBy;
}