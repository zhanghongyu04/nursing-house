package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

 /**
 * 养老院图片
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("tb_sana_image")
public class SanaImage extends BaseEntity {
    /**
     * 图片地址
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 养老院id
     */
    @TableField("sana_id")
    private Long sanaId;

    /**
     * 养老院名称（非数据库字段）
     */
    @TableField(exist = false)
    private String sanaName;

    /**
     * 重写remark字段，标记为非数据库字段（tb_sana_image表没有remark字段）
     */
    @TableField(exist = false)
    private String remark;
}
