package com.zhiling.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 养老院分页查询DTO
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SanatoriumPageQueryDto implements Serializable {
    private Integer page; //页码
    private Integer pageSize; //每页记录数
    private String sanaName; //养老院名称
    private String sanaAffiliation; //所属区划
    private String sanaAddress; //养老院地址
    private Integer age; //年龄
    private Integer status; //运营状态 (0 - 正常运营，1 - 停业整顿，2 - 注销取缔)
    private Long sanaId; //机构ID（数据范围）
    private List<Long> sanatoriumIds; //选中的机构ID列表（导出使用）
    private Set<Long> sanaScopeIds; //机构范围ID集合（多机构授权）
}
