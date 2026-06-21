package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.PageResult;
import com.zhiling.common.result.Result;
import com.zhiling.model.dto.UserDto;
import com.zhiling.model.dto.UserPageQueryDto;
import com.zhiling.system.admin.service.AdminDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/admin")
@Slf4j
@Tag(name = "管理员权限", description = "管理员权限")
/**
 * AdminController
 *
 * @author zhanghongyu
 */
public class AdminController {

    private final AdminDomainService adminDomainService;

    /**
     * 构造器：AdminController
     *
     * @author zhanghongyu
     */
    public AdminController(AdminDomainService adminDomainService) {
        this.adminDomainService = adminDomainService;
    }

    /**
     * 方法：addUser
     *
     * @author zhanghongyu
     */
    @PostMapping("/add")
    @Operation(summary = "添加用户")
    public Result<Boolean> addUser(@RequestBody UserDto userDto) {
        return Result.success(adminDomainService.addUser(userDto));
    }

    /**
     * 方法：RequestParam
     *
     * @author zhanghongyu
     */
    @PostMapping("/delete")
    @Operation(summary = "删除用户")
    public Result<Boolean> deleteUser(@RequestParam("username") String username) {
        return Result.success(adminDomainService.deleteUser(username));
    }

    /**
     * 方法：updateUser
     *
     * @author zhanghongyu
     */
    @PostMapping("/update")
    @Operation(summary = "更新用户信息")
    public Result<Boolean> updateUser(@RequestBody UserDto userDto) {
        return Result.success(adminDomainService.updateUser(userDto));
    }

    /**
     * 方法：resetPassword
     *
     * @author zhanghongyu
     */
    @PostMapping("/resetPassword")
    @Operation(summary = "重置用户密码")
    public Result<Boolean> resetPassword(@RequestBody UserDto userDto) {
        return Result.success(adminDomainService.resetPassword(userDto));
    }

    /**
     * 方法：pageUser
     *
     * @author zhanghongyu
     */
    @PostMapping("/pageUser")
    @Operation(summary = "分页查询用户列表")
    public Result<PageResult> pageUser(@RequestBody UserPageQueryDto userDto) {
        return Result.success(adminDomainService.pageUser(userDto));
    }


}


