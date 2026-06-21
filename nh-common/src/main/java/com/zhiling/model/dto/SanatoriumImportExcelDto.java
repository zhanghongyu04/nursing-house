package com.zhiling.model.dto;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

/**
 * 康养机构信息批量导入行模型。
 *
 * 仅暴露业务用户可维护字段；数据库主键、层级、创建/更新时间等由系统处理。
 *
 * @author zhanghongyu
 */
@Data
@ExcelIgnoreUnannotated
@ColumnWidth(22)
@HeadRowHeight(20)
@HeadFontStyle(fontHeightInPoints = 11)
public class SanatoriumImportExcelDto {

    @ExcelProperty(value = "机构名称（必填）", index = 0)
    private String sanaName;

    @ExcelProperty(value = "所属区划", index = 1)
    private String sanaAffiliation;

    @ExcelProperty(value = "机构地址（必填）", index = 2)
    private String sanaAddress;

    @ExcelProperty(value = "运营状态", index = 3)
    private String status;

    @ExcelProperty(value = "统一社会信用代码（必填）", index = 4)
    private String uscc;

    @ExcelProperty(value = "法人姓名", index = 5)
    private String legalPersons;

    @ExcelProperty(value = "法人联系方式", index = 6)
    private String legalPhone;

    @ExcelProperty(value = "床位总数", index = 7)
    private String bedCount;

    @ExcelProperty(value = "已用床位数", index = 8)
    private String bedInUse;

    @ExcelProperty(value = "养老人员数量", index = 9)
    private String elderCount;

    @ExcelProperty(value = "护理人员数量", index = 10)
    private String nursingCount;

    @ExcelProperty(value = "医护人员数量", index = 11)
    private String medicalCount;
}
