package com.zhiling.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 护理日志新增DTO
 *
 * @author zhanghongyu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NursingLogAddDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long sanaId;
    private Long taskId;
    private Long elderId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime logTime;
    private String content;
    private Integer abnormalFlag;
    private String attachmentUrls;
}
