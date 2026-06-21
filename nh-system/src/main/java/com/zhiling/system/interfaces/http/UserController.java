package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.Result;
import com.zhiling.common.utils.AddressUtil;
import com.zhiling.common.utils.IPUtil;
import com.zhiling.model.dto.PasswordResetDto;
import com.zhiling.model.vo.UserNavVo;
import com.zhiling.system.application.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/user")
@Slf4j
@Tag(name = "用户管理", description = "用户管理")
/**
 * UserController
 *
 * @author zhanghongyu
 */
public class UserController {
    private final UserService userService;

    /**
     * 构造器：UserController
     *
     * @author zhanghongyu
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 方法：getUserNavInfo
     *
     * @author zhanghongyu
     */
    @GetMapping("/getUserNavInfo")
    @Operation(summary = "获取用户中心信息")
    public Result<UserNavVo> getUserNavInfo() {
        return Result.success(userService.getUserNavInfo());
    }

    /**
     * 方法：resetPassword
     *
     * @author zhanghongyu
     */
    @PostMapping("/resetPassword")
    @Operation(summary = "重置密码")
    public Result<Boolean> resetPassword(@RequestBody PasswordResetDto passwordResetDto) {
        return Result.success(userService.resetPassword(passwordResetDto.getOldPassword(), passwordResetDto.getNewPassword()));
    }

    /**
     * 方法：updateUserInfo
     *
     * @author zhanghongyu
     */
    @PostMapping("/updateUserInfo")
    @Operation(summary = "更新用户信息")
    public Result<Boolean> updateUserInfo(@RequestBody UserNavVo userNavVo) {
        return Result.success(userService.updateUserInfo(userNavVo));
    }



}


