package com.zhiling.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * 养老院图片分页查询DTO
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SanaImagePageQueryDto implements Serializable {
    private Integer page; //页码
    private Integer pageSize; //每页记录数
    private String sanaName; //养老院名称
    private Long sanaId; //养老院id
    private Set<Long> sanaScopeIds; //机构范围ID集合（多机构授权）

}