package com.zhiling.system.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.common.security.LoginVo;
import com.zhiling.model.entity.Role;
import com.zhiling.model.entity.User;
import com.zhiling.model.vo.UserNavVo;
import com.zhiling.system.application.service.ResourceService;
import com.zhiling.system.application.service.RoleService;
import com.zhiling.system.application.service.UserService;
import com.zhiling.system.application.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户服务实现
 *
 * @author zhanghongyu
 */
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final ResourceService resourceService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecurityHelper securityHelper;

    public UserServiceImpl(UserRepository userRepository,
                          RoleService roleService,
                          ResourceService resourceService,
                          BCryptPasswordEncoder passwordEncoder,
                          SecurityHelper securityHelper) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.resourceService = resourceService;
        this.passwordEncoder = passwordEncoder;
        this.securityHelper = securityHelper;
    }

    /***
     *  查询用户构建对象
     * @param username
     * @return
     */
    public LoginVo findUserVoForLogin(String username) {
        User user = userRepository.selectByUsername(username);
        if (!ObjectUtil.isEmpty(user)){
            return BeanUtil.toBean(user,LoginVo.class);
        }
        throw new ProjectException(401, "用户名或密码错误");
    }

    /**
     * 获取用户中心信息
     * @return
     */
    public UserNavVo getUserNavInfo() {
        Long id = securityHelper.getCurrentUserId();
        UserNavVo userNavVo = userRepository.getUserNavInfo(id);
        if (userNavVo == null) {
            throw new ProjectException(404, "用户信息不存在");
        }
        userNavVo.setUserId(id);
        List<Role> roleList = roleService.getRoleListByUserId(String.valueOf(id));
        Set<String> roleLabels = new LinkedHashSet<>();
        for (Role role : roleList) {
            roleLabels.add(role.getLabel());
            if (userNavVo.getRoleId() == null) {
                userNavVo.setRoleId(role.getId());
            }
        }
        userNavVo.setRoleLabels(roleLabels);
        if (!roleLabels.isEmpty()) {
            userNavVo.setPrimaryRole(roleLabels.iterator().next());
        }
        userNavVo.setSanaScopeIds(securityHelper.getCurrentSanaScopeIds());
        Set<String> resourcePaths = resourceService.getResourceListByUserId(String.valueOf(id)).stream()
                .filter(resource -> "m".equals(resource.getResourceType()) || "r".equals(resource.getResourceType()))
                .map(com.zhiling.model.entity.Resource::getRequestPath)
                .filter(path -> path != null && !path.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        userNavVo.setResourcePaths(resourcePaths);
        return userNavVo;
    }




    /**
     * 重置密码
     * @param newPassword
     * @return
     */
    @Override
    public Boolean resetPassword(String oldPassword,String newPassword) {
        Long userId = securityHelper.getCurrentUserId();
        String storedPassword = userRepository.getPassword(userId);
        if (!passwordEncoder.matches(oldPassword, storedPassword)) {
            throw new ProjectException(400, "原密码错误");
        }
        String encodedPassword = passwordEncoder.encode(newPassword);
        return userRepository.updatePasswordByUserId(userId, encodedPassword);
    }




    /**
     * 更新用户信息
     * @param userNavVo
     * @return
     */
    public Boolean updateUserInfo(UserNavVo userNavVo) {
        Long userId= securityHelper.getCurrentUserId();
        User user=User.builder()
               .username(userNavVo.getUsername())
               .avatar(userNavVo.getAvatar())
               .phoneNumber(userNavVo.getPhoneNumber())
              .email(userNavVo.getEmail())
              .build();
        user.setId(userId);
        Boolean flag=userRepository.updateById(user);
        return flag;
    }


}



