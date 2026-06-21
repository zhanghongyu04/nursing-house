package com.zhiling.model.entity;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhiling.common.enums.SanatoriumStatusEnum;
import com.zhiling.common.enums.SexEnum;
import com.zhiling.common.enums.handler.EnumConverter;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 养老院信息实体类
 *
 * @author zhanghongyu
 */
@Data
@ExcelIgnoreUnannotated// 注解表示在导出Excel时，忽略没有被任何注解标记的字段
@ColumnWidth(16)// 注解用于设置列的宽度
@HeadRowHeight(14)// 注解用于设置表头行的高度
@HeadFontStyle(fontHeightInPoints = 11)// 注解用于设置表头的字体样式
@TableName("tb_sanatorium")
public class Sanatorium implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 机构名称 */
    @TableField("sana_name")
    @ExcelProperty ("机构名称")
    private String sanaName;

    /** 所属区划 */
    @TableField("sana_affiliation")
    @ExcelProperty ("所属区划")
    private String sanaAffiliation;

    /** 养老院地址 */
    @TableField("sana_address")
    @ExcelProperty ("养老院地址")
    private String sanaAddress;

    /** 运营状态 (0 - 正常运营，1 - 停业整顿，2 - 注销取缔) */
    @TableField("status")
    @ExcelProperty (value = "运营状态", converter = SanatoriumStatusConverter.class)
    private Integer status;

    /** 统一社会信用代码 */
    @TableField("uscc")
    @ExcelProperty ("统一社会信用代码")
    private String uscc;

    /** 法人姓名 */
    @TableField("legal_persons")
    @ExcelProperty ("法人姓名")
    private String legalPersons;

    /** 法人联系方式 */
    @TableField("legal_phone")
    @ExcelProperty ("法人联系方式")
    private String legalPhone;

    /** 床位总数 */
    @TableField("bed_count")
    @ExcelProperty ("床位总数")
    private Integer bedCount;

    /** 已用床位数 */
    @TableField("bed_in_use")
    @ExcelProperty ("已用床位数")
    private Integer bedInUse;

    /** 养老人员数量 */
    @TableField("elder_count")
    @ExcelProperty ("养老人员数量")
    private Integer elderCount;

    /** 护理人员数 */
    @TableField("nursing_count")
    @ExcelProperty ("护理人员数量")
    private Integer nursingCount;

    /** 医护人员数 */
    @TableField("medical_count")
    @ExcelProperty ("医护人员数量")
    private Integer medicalCount;

    /** 上级机构ID（总部为空） */
    @TableField("parent_sana_id")
    private Long parentSanaId;

    /** 机构层级（1总部，2分支） */
    @TableField("org_level")
    private Integer orgLevel;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @DateTimeFormat ("yyyy-MM-dd HH:mm:ss")
    @JsonFormat (pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @DateTimeFormat ("yyyy-MM-dd HH:mm:ss")
    @JsonFormat (pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;



    /** 性别转换器 */
    public static class SanatoriumStatusConverter extends EnumConverter<SanatoriumStatusEnum> {
        /**
         * 方法：SanatoriumStatusConverter
         *
         * @author zhanghongyu
         */
        public SanatoriumStatusConverter() {
            // 传入枚举类和描述获取方法（使用方法引用）
            super(SanatoriumStatusEnum.class, SanatoriumStatusEnum::getDesc);
        }
    }



    // 运营状态转换方法（可选）
    /**
     * 方法：getStatusName
     *
     * @author zhanghongyu
     */
    public String getStatusName () {
        if (status == null) return "未知状态";
        switch (status) {
            case 0: return "正常运营";
            case 1: return "停业整顿";
            case 2: return "注销取缔";
            default: return "未知状态";
        }

    }
}