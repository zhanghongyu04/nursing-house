package com.zhiling.system.application.dto;

import lombok.Data;

@Data
/**
 * VideoPtzRequest
 *
 * @author zhanghongyu
 */
public class VideoPtzRequest {
    private Integer direction;
    private Integer speed = 2;
}

