package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.Result;
import com.zhiling.model.dto.LoginDto;
import com.zhiling.common.security.LoginVo;
import com.zhiling.system.application.service.LoginMonitorService;
import com.zhiling.system.auth.service.AuthDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "登录管理", description = "登录管理")
@Slf4j
/**
 * LoginController
 *
 * @author zhanghongyu
 */
public class LoginController {
    private final AuthDomainService authDomainService;
    private final LoginMonitorService loginMonitorService;

    /**
     * 构造器：LoginController
     *
     * @author zhanghongyu
     */
    public LoginController(AuthDomainService authDomainService, LoginMonitorService loginMonitorService) {
        this.authDomainService = authDomainService;
        this.loginMonitorService = loginMonitorService;
    }

    /**
     * 方法：login
     *
     * @author zhanghongyu
     */
    @PostMapping("/login")
    @Operation(summary = "登录")
    public Result<LoginVo> login(@RequestBody LoginDto loginDto, HttpServletRequest request) {
        try {
            LoginVo loginVo = authDomainService.login(loginDto);
            loginMonitorService.recordSuccess(loginVo, request);
            return Result.success(loginVo);
        } catch (RuntimeException ex) {
            loginMonitorService.recordFailure(loginDto == null ? null : loginDto.getUsername(), ex.getMessage(), request);
            throw ex;
        }
    }

    /**
     * 方法：logout
     *
     * @author zhanghongyu
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<Boolean> logout() {
        return Result.success(authDomainService.logout());
    }




}



