package com.zhiling.model.dto;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 老人信息DTO
 *
 * @author zhanghongyu
 */
@Data
@ExcelIgnoreUnannotated
@ColumnWidth(16)
@HeadRowHeight(14)
@HeadFontStyle(fontHeightInPoints = 11)
public class ElderDto {

    /** 老人ID */
    private Long id;

    /** 所属养老院名称 */
    private String sanaName;
    private Long sanaId; // 所属养老院id
    /** 养老人员姓名 */
    private String elderName;
    /** 性别 */
    private Integer sex;
    private Integer age; // 年龄

    /** 身份证号 */
    private String idNumber;

    /** 手机号码 */
    private String phoneNumber;

    /** 家庭住址 */
    private String homeAddress;

    /** 入住类型（0-社会老年人,1-低保,2-特困,3-建档立卡） */
    private Integer familySituation;

    /** 占用床位类型 */
    private Integer occupiedBedType;

    /** 房间号 */
    private String roomNumber;

    /** 担保人/监护人姓名 */
    private String guardianName;

    /** 担保人手机号 */
    private String guardianPhone;

    /** 自理能力（0-能力完好,1-轻度失能,2-中度失能,3-重度失能,4-完全失能） */
    private Integer selfCare;

    /** 入院时间 */
    @ExcelProperty("入院时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd",       // 配置 JSON 序列化/反序列化格式
            timezone = "GMT+8",           // 时区（避免时间偏移）
            shape = JsonFormat.Shape.STRING // 强制序列化为字符串格式
    )
    private LocalDate inpatientsTime;

    /** 出院时间 */
    @ExcelProperty("出院时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd",
            timezone = "GMT+8",
            shape = JsonFormat.Shape.STRING
    )
    private LocalDate outpatientsTime;

    /** 收费标准 */
    private Float fees;


}
