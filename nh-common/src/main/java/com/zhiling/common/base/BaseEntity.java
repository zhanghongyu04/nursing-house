package com.zhiling.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 基础字段
 *
 * @author zhanghongyu
 */
@Data
public class BaseEntity {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(value = "create_by", exist = false)
    private Long createBy;

    /**
     * 更新人
     */
    @TableField(value = "update_by", exist = false)
    private Long updateBy;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 状态 0：正常 1：停用
     */
    @TableField(value = "status")
    private Integer status;
}
