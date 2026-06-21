package com.zhiling.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiling.model.entity.Camera;
import com.zhiling.model.entity.VideoAlert;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 视频监控 Mapper
 *
 * @author zhanghongyu
 */
@Mapper
public interface VideoMapper extends BaseMapper<Camera> {

    /**
     * 根据养老院ID查询摄像头列表。
     */
    @Select("SELECT " +
            "c.id, " +
            "c.sana_id, " +
            "s.sana_name, " +
            "c.camera_name, " +
            "c.camera_location, " +
            "c.camera_status, " +
            "c.device_serial, " +
            "c.channel_no, " +
            "c.validate_code, " +
            "c.stream_quality, " +
            "c.online_status, " +
            "c.create_time, " +
            "c.update_time " +
            "FROM tb_camera c " +
            "LEFT JOIN tb_sanatorium s ON c.sana_id = s.id " +
            "WHERE c.sana_id = #{sanaId} " +
            "ORDER BY c.update_time DESC, c.id DESC")
    List<Camera> listBySanaId(Long sanaId);

    @Select("SELECT " +
            "va.id, " +
            "va.sana_id, " +
            "s.sana_name, " +
            "va.camera_id, " +
            "c.camera_name, " +
            "c.camera_location, " +
            "va.content, " +
            "va.alert_time, " +
            "va.status, " +
            "va.handled_by, " +
            "va.handled_time, " +
            "va.handle_remark " +
            "FROM tb_video_alert va " +
            "LEFT JOIN tb_camera c ON va.camera_id = c.id " +
            "LEFT JOIN tb_sanatorium s ON va.sana_id = s.id " +
            "WHERE va.id = #{alertId}")
    VideoAlert getAlertById(Long alertId);

    @Select({
            "<script>",
            "SELECT id, sana_id, camera_name, camera_location, camera_status, ",
            "device_serial, channel_no, validate_code, stream_quality, online_status, create_time, update_time ",
            "FROM tb_camera ",
            "WHERE device_serial = #{deviceSerial} ",
            "<if test='channelNo != null'>",
            "AND channel_no = #{channelNo} ",
            "</if>",
            "<if test='channelNo == null'>",
            "ORDER BY CASE WHEN channel_no = 1 THEN 0 ELSE 1 END, channel_no ASC ",
            "</if>",
            "LIMIT 1",
            "</script>"
    })
    Camera getCameraByDeviceSerialAndChannelNo(@Param("deviceSerial") String deviceSerial,
                                               @Param("channelNo") Integer channelNo);

    /**
     * 根据养老院 ID 精确查询告警信息。
     */
    @Select("SELECT " +
            "va.id, " +
            "va.sana_id, " +
            "s.sana_name, " +
            "va.camera_id, " +
            "c.camera_name, " +
            "c.camera_location, " +
            "va.content, " +
            "va.alert_time, " +
            "va.status, " +
            "va.handled_by, " +
            "va.handled_time, " +
            "va.handle_remark, " +
            "va.create_time, " +
            "va.update_time " +
            "FROM tb_video_alert va " +
            "LEFT JOIN tb_camera c ON va.camera_id = c.id " +
            "LEFT JOIN tb_sanatorium s ON va.sana_id = s.id " +
            "WHERE va.sana_id = #{sanaId} " +
            "ORDER BY va.alert_time DESC")
    List<VideoAlert> getAlertsBySanaId(Long sanaId);

    @Update("UPDATE tb_video_alert " +
            "SET status = 1, handled_by = #{handledBy}, handled_time = NOW(), handle_remark = #{handleRemark} " +
            "WHERE id = #{alertId} AND status = 0")
    int handleAlert(@Param("alertId") Long alertId,
                    @Param("handledBy") Long handledBy,
                    @Param("handleRemark") String handleRemark);

    @Insert("INSERT INTO tb_video_alert (sana_id, camera_id, content, alert_time, status) " +
            "VALUES (#{sanaId}, #{cameraId}, #{content}, #{alertTime}, 1)")
    int insertStatusLog(@Param("sanaId") Long sanaId,
                        @Param("cameraId") Long cameraId,
                        @Param("content") String content,
                        @Param("alertTime") java.time.LocalDateTime alertTime);
}

