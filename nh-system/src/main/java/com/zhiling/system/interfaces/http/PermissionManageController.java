package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.Result;
import com.zhiling.model.dto.RoleResourceAssignDto;
import com.zhiling.model.entity.Role;
import com.zhiling.model.vo.PermissionResourceTreeVo;
import com.zhiling.system.application.service.PermissionManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理控制器。
 *
 * @author zhanghongyu
 */
@RestController
@RequestMapping("/api/v1/permission")
@Tag(name = "权限管理", description = "角色资源授权管理")
public class PermissionManageController {

    private final PermissionManageService permissionManageService;

    public PermissionManageController(PermissionManageService permissionManageService) {
        this.permissionManageService = permissionManageService;
    }

    @GetMapping("/roles")
    @Operation(summary = "查询角色列表")
    public Result<List<Role>> listRoles() {
        return Result.success(permissionManageService.listRoles());
    }

    @GetMapping("/resources/tree")
    @Operation(summary = "查询资源树")
    public Result<List<PermissionResourceTreeVo>> listResourceTree() {
        return Result.success(permissionManageService.listResourceTree());
    }

    @GetMapping("/roles/{roleId}/resources")
    @Operation(summary = "查询角色已授权资源")
    public Result<List<String>> listRoleResources(@PathVariable("roleId") Long roleId) {
        return Result.success(permissionManageService.listRoleResourceNos(roleId));
    }

    @PutMapping("/roles/{roleId}/resources")
    @Operation(summary = "保存角色资源授权")
    public Result<Boolean> saveRoleResources(@PathVariable("roleId") Long roleId,
                                             @RequestBody RoleResourceAssignDto assignDto) {
        List<String> resourceNos = assignDto == null ? null : assignDto.getResourceNos();
        return Result.success(permissionManageService.saveRoleResources(roleId, resourceNos));
    }
}
