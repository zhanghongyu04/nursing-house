package com.zhiling.model.dto;

import lombok.Data;

@Data
/**
 * PasswordResetDto
 *
 * @author zhanghongyu
 */
public class PasswordResetDto {
    private String oldPassword;
    private String newPassword;
}

