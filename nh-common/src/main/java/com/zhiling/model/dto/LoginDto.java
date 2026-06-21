package com.zhiling.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

 /**
 * 登录dto
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginDto {
    private String username;
    private String password;

    /**
     * 验证码 key（从响应头获取）
     */
    private String captchaKey;

    /**
     * 用户输入的验证码
     */
    private String captchaCode;
}
