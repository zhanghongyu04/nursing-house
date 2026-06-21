package com.zhiling.common.enums;

/**
 *  养老院运营状态枚举
 *
 * @author zhanghongyu
 */

public enum SanatoriumStatusEnum {
    NORMAL_OPERATION(0, "正常运营"),
    SUSPENDED(1, "停业整顿"),
    CANCELLED(2, "注销取缔");

    private final Integer code;
    private final String desc;

    SanatoriumStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }
}
