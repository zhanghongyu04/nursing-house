package com.zhiling.model.dto;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

/**
 * 老人信息 Excel 导入导出行模型。
 *
 * @author zhanghongyu
 */
@Data
@ExcelIgnoreUnannotated
@ColumnWidth(18)
@HeadRowHeight(16)
@HeadFontStyle(fontHeightInPoints = 11)
public class ElderExcelDto {

    @ExcelProperty(value = "所属养老院名称", index = 0)
    private String sanaName;

    @ExcelProperty(value = "养老人员姓名", index = 1)
    private String elderName;

    @ExcelProperty(value = "性别", index = 2)
    private String sex;

    @ExcelProperty(value = "年龄", index = 3)
    private String age;

    @ExcelProperty(value = "身份证号", index = 4)
    private String idNumber;

    @ExcelProperty(value = "手机号码", index = 5)
    private String phoneNumber;

    @ExcelProperty(value = "家庭住址", index = 6)
    private String homeAddress;

    @ExcelProperty(value = "入住类型", index = 7)
    private String familySituation;

    @ExcelProperty(value = "占用床位类型", index = 8)
    private String occupiedBedType;

    @ExcelProperty(value = "房间号", index = 9)
    private String roomNumber;

    @ExcelProperty(value = "监护人姓名", index = 10)
    private String guardianName;

    @ExcelProperty(value = "监护人手机号", index = 11)
    private String guardianPhone;

    @ExcelProperty(value = "自理能力", index = 12)
    private String selfCare;

    @ExcelProperty(value = "入院时间", index = 13)
    private String inpatientsTime;

    @ExcelProperty(value = "出院时间", index = 14)
    private String outpatientsTime;

    @ExcelProperty(value = "收费标准", index = 15)
    private String fees;
}
