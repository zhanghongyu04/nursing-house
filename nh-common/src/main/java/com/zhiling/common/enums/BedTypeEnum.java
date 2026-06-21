package com.zhiling.common.enums;

/**
 * 床位类型枚举
 * 0-普通床位，1-护理型床位
 *
 * @author zhanghongyu
 */
public enum BedTypeEnum {
    NORMAL(0, "普通床位"),
    NURSING(1, "护理型床位");

    private final Integer code;
    private final String desc;

    BedTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 方法：getCode
     *
     * @author zhanghongyu
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 方法：getDesc
     *
     * @author zhanghongyu
     */
    public String getDesc() {
        return desc;
    }
}