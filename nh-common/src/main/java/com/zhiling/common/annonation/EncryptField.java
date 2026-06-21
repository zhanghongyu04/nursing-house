package com.zhiling.common.annonation;

import java.lang.annotation.*;

 /**
 * 标识加密字段注解
 * @author zhanghongyu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EncryptField {
}

