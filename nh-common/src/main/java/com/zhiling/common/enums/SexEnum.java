package com.zhiling.common.enums;

 /**
 *  性别枚举
 *
 * @author zhanghongyu
 */
public enum SexEnum {
    MALE(0, "男"),
    FEMALE(1, "女"),
    UNKNOWN(2, "未知");

    private final Integer code;
    private final String desc;

    SexEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }
}
