package com.zhiling.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

 /**
 * 用户中心信息
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserNavVo {
   private Long userId;
   private String username;
   private String avatar;
   private Long roleId;
   private Set<String> roleLabels;
   private String primaryRole;
   private String email;
   private String phoneNumber;
   private Long sanaId;
   private Set<Long> sanaScopeIds;
   private Set<String> resourcePaths;
}
