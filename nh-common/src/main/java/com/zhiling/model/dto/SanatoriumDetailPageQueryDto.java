package com.zhiling.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * 养老院分页详情老人信息分页查询DTO
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SanatoriumDetailPageQueryDto implements Serializable {
    private Integer page; //页码
    private Integer pageSize; //每页记录数
    private String sanaName; //养老院名称
    private Integer selfCare; //自理能力
    private Long sanaId; //机构ID（数据范围）
    private Set<Long> sanaScopeIds; //机构范围ID集合（多机构授权）
}