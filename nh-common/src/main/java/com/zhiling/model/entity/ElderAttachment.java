package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 老人档案附件。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("tb_elder_attachment")
public class ElderAttachment extends BaseEntity {

    /** 老人ID */
    @TableField("elder_id")
    private Long elderId;

    /** 原始文件名 */
    @TableField("file_name")
    private String fileName;

    /** 文件URL */
    @TableField("file_url")
    private String fileUrl;

    /** 文件MIME类型 */
    @TableField("file_type")
    private String fileType;

    /** 文件大小 */
    @TableField("file_size")
    private Long fileSize;

    /** 附件类型：0图片，1普通附件 */
    @TableField("attachment_type")
    private Integer attachmentType;
}
