package com.zhiling.common.enums;

/**
 *  入住类型枚举
 *
 * @author zhanghongyu
 */
public enum FamilySituationEnum {
    SOCIAL_ELDER(0, "社会老年人"),
    LOW_INCOME(1, "低保老年人"),
    EXTREMELY_POOR(2, "特困"),
    FILED_CARD(3, "建档立卡");

    private final Integer code;
    private final String desc;

    FamilySituationEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }
}
