package com.zhiling.model.entity;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.converters.localdate.LocalDateStringConverter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhiling.common.annonation.EncryptField;
import com.zhiling.common.enums.BedTypeEnum;
import com.zhiling.common.enums.FamilySituationEnum;
import com.zhiling.common.enums.SelfCareEnum;
import com.zhiling.common.enums.SexEnum;
import com.zhiling.common.enums.handler.EnumConverter;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * 养老人员信息实体类
 *
 * @author zhanghongyu
 */
@Data
@ExcelIgnoreUnannotated
@ColumnWidth(16)
@HeadRowHeight(14)
@HeadFontStyle(fontHeightInPoints = 11)
@TableName("tb_elder")
public class Elder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属养老院ID */
    @TableField("sana_id")
    private Long sanaId;

    /** 所属养老院名称 */
    @TableField(exist = false) // 非数据库字段
    @ExcelProperty("所属养老院名称")
    private String sanaName;

    /** 养老人员姓名 */
    @TableField("elder_name")
    @ExcelProperty("养老人员姓名")
    private String elderName;

    /** 性别（0男 1女 2未知） */
    @TableField("sex")
    @ExcelProperty(
            value = "性别",
            converter = SexConverter.class // 使用通用枚举转换器
    )
    private Integer sex;

    /** 年龄 */
    @TableField("age")
    @ExcelProperty("年龄")
    private Integer age;

    /** 身份证号 */
    @TableField("id_number")
    @ExcelProperty("身份证号")
    @EncryptField
    private String idNumber;

    /** 手机号码 */
    @TableField("phone_number")
    @ExcelProperty("手机号码")
    @EncryptField
    private String phoneNumber;

    /** 家庭住址 */
    @TableField("home_address")
    @ExcelProperty("家庭住址")
    @EncryptField
    private String homeAddress;

    /** 入住类型（0-社会老年人,1-低保,2-特困,3-建档立卡） */
    @TableField("family_situation")
    @ExcelProperty(
            value = "入住类型",
            converter = FamilySituationConverter.class
    )
    private Integer familySituation;

    /** 占用床位类型 */
    @TableField("occupied_bed_type")
    @ExcelProperty(
            value = "占用床位类型",
            converter = BedTypeConverter.class
    )
    private Integer occupiedBedType;

    /** 房间号 */
    @TableField("room_number")
    @ExcelProperty("房间号")
    private String roomNumber;

    /** 担保人/监护人姓名 */
    @TableField("guardian_name")
    @ExcelProperty("监护人姓名")
    private String guardianName;

    /** 担保人手机号 */
    @TableField("guardian_phone")
    @ExcelProperty("监护人手机号")
    private String guardianPhone;

    /** 自理能力（0-能力完好,1-轻度失能,2-中度失能,3-重度失能,4-完全失能） */
    @TableField("self_care")
    @ExcelProperty(
            value = "自理能力",
            converter = SelfCareConverter.class
    )
    private Integer selfCare;

//     /** 入院时间 */
//     @ExcelProperty("入院时间")
//     @JsonFormat(
//             pattern = "yyyy-MM-dd",       // 配置 JSON 序列化/反序列化格式
//             timezone = "GMT+8",           // 时区（避免时间偏移）
//             shape = JsonFormat.Shape.STRING // 强制序列化为字符串格式
//     )
//     private LocalDate inpatientsTime;
//
//     /** 出院时间 */
//     @ExcelProperty("出院时间")
//     @JsonFormat(
//             pattern = "yyyy-MM-dd",
//             timezone = "GMT+8",
//             shape = JsonFormat.Shape.STRING
//     )
//     private LocalDate outpatientsTime;
    /** 收费标准 */
    @TableField("fees")
    @ExcelProperty(value = "收费标准",converter = FeesConverter.class)
    private Float fees;
    // 自定义收费标准转换器（处理浮点数转换问题）
    public static class FeesConverter implements Converter<Float> {
        @Override
        public Float convertToJavaData(
                ReadCellData<?> cellData,
                ExcelContentProperty contentProperty,
                GlobalConfiguration globalConfiguration) {

            // 1. 处理空值
            if (cellData == null) {
                return null;
            }
            String cellValue = cellData.getStringValue();
            if (cellValue == null || cellValue.trim().isEmpty()) {
                return null;
            }
            cellValue = cellValue.trim();

            // 2. 处理特殊值（如"0"、"-"等无意义值）
            if ("0".equals(cellValue) || "-".equals(cellValue) || "无".equals(cellValue)) {
                return null; // 视为空值处理
            }

            // 3. 清理非法字符（只保留数字、小数点和开头的负号）
            String cleanValue = cellValue
                    .replaceAll("[^-0-9.]", "") // 移除所有非数字、非小数点、非负号的字符
                    .replaceAll("-(?=-)", "")   // 移除连续的负号（如"--123" → "-123"）
                    .replaceAll("-(?!^)", "");  // 移除非开头的负号（如"123-45" → "12345"）

            // 4. 处理多个小数点（如"123.45.67" → "123.4567"）
            if (cleanValue.indexOf('.') != cleanValue.lastIndexOf('.')) {
                int firstDot = cleanValue.indexOf('.');
                cleanValue = cleanValue.substring(0, firstDot + 1)
                        + cleanValue.substring(firstDot + 1).replace(".", "");
            }

            // 5. 尝试转换为Float
            try {
                if (cleanValue.isEmpty()) {
                    return null;
                }
                return Float.parseFloat(cleanValue);
            } catch (NumberFormatException e) {
                // 转换失败时，抛出包含原始值的异常，方便定位问题
                throw new NumberFormatException("收费标准格式错误，原始值：" + cellValue + "，清理后：" + cleanValue);
            }
        }

        @Override
        public WriteCellData<?> convertToExcelData(
                Float value,
                ExcelContentProperty contentProperty,
                GlobalConfiguration globalConfiguration) {
            // 导出时格式化显示（保留2位小数，符合金额格式）
            if (value == null) {
                return new WriteCellData<>("");
            }
            return new WriteCellData<>(String.format("%.2f", value));
        }

        /**
         * 方法：supportJavaTypeKey
         *
         * @author zhanghongyu
         */
        @Override
        public Class<?> supportJavaTypeKey() {
            return Float.class;
        }

        /**
         * 方法：supportExcelTypeKey
         *
         * @author zhanghongyu
         */
        @Override
        public CellDataTypeEnum supportExcelTypeKey() {
            return CellDataTypeEnum.STRING;
        }
    }

    /** 状态（0-正常，1-禁用） */
    @TableField("status")
    private Integer status;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 入院时间 - 关键：添加converter指定自定义转换器 */
    @TableField("inpatients_time")
    @ExcelProperty(value = "入院时间", converter = LocalDateConverter.class)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    private LocalDate inpatientsTime;

    /** 出院时间 - 同理添加转换器 */
    @TableField("outpatients_time")
    @ExcelProperty(value = "出院时间", converter = LocalDateConverter.class)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    private LocalDate outpatientsTime;

    /** 入住天数（当前系统日期 - 入院时间，非数据库字段） */
    @TableField(exist = false)
    private Long inpatientDays;

    /** 入住时长展示文案（如：30、已出院），非数据库字段 */
    @TableField(exist = false)
    private String inpatientDaysLabel;




    /**
     * 正确的日期转换器实现
     * 严格遵循Converter接口规范，特别是返回值类型
     */
    public static class LocalDateConverter implements Converter<LocalDate> {
        // 支持的日期格式（包含单数月/日的宽松格式）
        private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
                // 新增：支持点分隔符格式（如2019.4.2、2019.04.02）
                DateTimeFormatter.ofPattern("yyyy.M.d"),    // 匹配 2019.4.2
                DateTimeFormatter.ofPattern("yyyy.MM.dd"),  // 匹配 2019.04.02
                // 原有格式保持不变
                DateTimeFormatter.ofPattern("yyyy-M-d"),    // 支持 2018-9-28
                DateTimeFormatter.ofPattern("yyyy/M/d"),    // 支持 2018/9/28
                DateTimeFormatter.ofPattern("d/M/yyyy"),    // 支持 28/9/2018
                DateTimeFormatter.ofPattern("M/d/yyyy"),    // 支持 9/28/2018
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),  // 支持 2018-09-28
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),  // 支持 2018/09/28
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),  // 支持 28/09/2018
                DateTimeFormatter.ofPattern("MM/dd/yyyy")   // 支持 09/28/2018
        );

        /**
         * 导入时：Excel单元格数据 -> Java LocalDate对象
         */
        @Override
        public LocalDate convertToJavaData(
                com.alibaba.excel.metadata.data.ReadCellData<?> cellData,
                ExcelContentProperty contentProperty,
                GlobalConfiguration globalConfiguration) {

            // 处理空值
            if (cellData == null) {
                return null;
            }
            String dateStr = cellData.getStringValue();
            // 新增：如果值为"0"，直接返回null（置空）
            if (dateStr != null && "0".equals(dateStr.trim())) {
                return null;
            }
            if (dateStr == null || dateStr.trim().isEmpty()) {
                return null;
            }
            dateStr = dateStr.trim();

            // 尝试匹配所有支持的格式
            for (DateTimeFormatter formatter : FORMATTERS) {
                try {
                    return LocalDate.parse(dateStr, formatter);
                } catch (DateTimeParseException e) {
                    continue; // 格式不匹配则尝试下一种
                }
            }

            // 所有格式都不匹配时抛出异常
            throw new DateTimeParseException(
                    "不支持的日期格式: " + dateStr + "（支持格式：2018-9-28、2018/09/28等）",
                    dateStr, 0
            );
        }

        /**
         * 导出时：Java LocalDate对象 -> Excel单元格数据
         * 关键：返回值必须是WriteCellData<?>类型
         */
        @Override
        public WriteCellData<?> convertToExcelData(
                LocalDate value,
                ExcelContentProperty contentProperty,
                GlobalConfiguration globalConfiguration) {

            // 空值处理
            if (value == null) {
                return new WriteCellData<>("");
            }

            // 转换为标准格式的字符串
            String dateStr = value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return new WriteCellData<>(dateStr);
        }

        /**
         * 声明支持的Java类型
         */
        @Override
        public Class<?> supportJavaTypeKey() {
            return LocalDate.class;
        }

        /**
         * 声明支持的Excel数据类型
         */
        @Override
        public CellDataTypeEnum supportExcelTypeKey() {
            return CellDataTypeEnum.STRING;
        }
    }


























// ====================== 转换器实现 ======================
     /** 性别转换器 */
     public static class SexConverter extends EnumConverter<SexEnum> {
         /**
          * 方法：SexConverter
          *
          * @author zhanghongyu
          */
         public SexConverter() {
             // 传入枚举类和描述获取方法（使用方法引用）
             super(SexEnum.class, SexEnum::getDesc);
         }
     }

     /** 入住类型转换器 */
     public static class FamilySituationConverter extends EnumConverter<FamilySituationEnum> {
         /**
          * 方法：FamilySituationConverter
          *
          * @author zhanghongyu
          */
         public FamilySituationConverter() {
             super(FamilySituationEnum.class, FamilySituationEnum::getDesc);
         }
     }

     /** 床位类型转换器 */
     public static class BedTypeConverter extends EnumConverter<BedTypeEnum> {
         /**
          * 方法：BedTypeConverter
          *
          * @author zhanghongyu
          */
         public BedTypeConverter() {
             super(BedTypeEnum.class, BedTypeEnum::getDesc);
         }
     }

     /** 自理能力转换器 */
     public static class SelfCareConverter extends EnumConverter<SelfCareEnum> {
         /**
          * 方法：SelfCareConverter
          *
          * @author zhanghongyu
          */
         public SelfCareConverter() {
             super(SelfCareEnum.class, SelfCareEnum::getDesc);
         }
     }








    // 性别转换方法
    /**
     * 方法：getSexName
     *
     * @author zhanghongyu
     */
    public String getSexName() {
        if (sex == null) return "未知";
        switch (sex) {
            case 0: return "男";
            case 1: return "女";
            default: return "未知";
        }
    }

    // 入住类型转换方法
    /**
     * 方法：getFamilySituationName
     *
     * @author zhanghongyu
     */
    public String getFamilySituationName() {
        if (familySituation == null) return "未知";
        switch (familySituation) {
            case 0: return "社会老年人";
            case 1: return "低保";
            case 2: return "特困老年人";
            case 3: return "建档立卡";
            default: return "未知";
        }
    }

    // 自理能力转换方法
    /**
     * 方法：getSelfCareName
     *
     * @author zhanghongyu
     */
    public String getSelfCareName() {
        if (selfCare == null) return "未知";
        switch (selfCare) {
            case 0: return "能力完好";
            case 1: return "轻度";
            case 2: return "中度";
            case 3: return "重度";
            case 4: return "完全失能";
            default: return "未知";
        }
    }

    // 床位类型转换方法
    /**
     * 方法：getOccupiedBedTypeName
     *
     * @author zhanghongyu
     */
    public String getOccupiedBedTypeName() {
        if (occupiedBedType == null) return "未知";
        switch (occupiedBedType) {
            case 0: return "普通床位";
            case 1: return "护理型床位";
            default: return "未知";
        }
    }

    // 状态转换方法
    /**
     * 方法：getStatusName
     *
     * @author zhanghongyu
     */
    public String getStatusName() {
        if (status == null) return "未知";
        return status == 0 ? "正常" : "禁用";
    }


 }
