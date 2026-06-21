package com.zhiling.model.dto;

import com.zhiling.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

 /**
 * 用户实体类
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto extends User {
    private Long roleId; // 角色id
    private Set<Long> sanaScopeIds; // 多机构授权范围
}