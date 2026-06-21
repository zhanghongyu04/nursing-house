package com.zhiling.common.enums;
/**
 *  自理能力枚举
 *
 * @author zhanghongyu
 */
public enum SelfCareEnum {
    GOOD(0, "能力完好"),
    MILD_DISABLED(1, "轻度失能"),
    MODERATE_DISABLED(2, "中度失能"),
    SEVERE_DISABLED(3, "重度失能"),
    COMPLETE_DISABLED(4, "完全失能");

    private final Integer code;
    private final String desc;

    SelfCareEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }
}
