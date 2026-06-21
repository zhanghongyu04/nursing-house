package com.zhiling.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

 /**
 * 用户列表分页查询DTO
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPageQueryDto {
    private Integer page; //页码
    private Integer pageSize; //每页记录数
    private String username; //用户姓名
    private Long sanaId; //机构ID（数据范围）
    private Set<Long> sanaScopeIds; //机构范围ID集合（多机构授权）
    private Long roleId; //角色ID（按角色筛选）
}