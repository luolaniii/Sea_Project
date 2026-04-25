package com.boot.study.controller.common;

import com.boot.study.annotation.PassToken;
import com.boot.study.api.req.LoginReq;
import com.boot.study.bean.Result;
import com.boot.study.bean.UserInfo;
import com.boot.study.service.biz.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证相关接口控制器
 * <p>
 * 提供用户登录、登出等认证相关的接口。
 * 使用@PassToken注解，表示这些接口不需要Token验证。
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/common/auth")
@RequiredArgsConstructor
@PassToken
public class AuthController {

    /**
     * 认证服务
     */
    private final AuthService authService;

    /**
     * 用户登录接口
     * <p>
     * 根据用户名和密码进行登录验证，登录成功后返回用户信息和Token。
     *
     * @param req 登录请求，包含用户名和密码
     * @return 用户信息和Token
     */
    @PostMapping("/login")
    public Result<UserInfo> login(@RequestBody LoginReq req) {
        log.info("用户登录请求，用户名: {}", req.getUsername());
        return authService.login(req);
    }

    /**
     * 用户登出接口
     * <p>
     * 清除用户的登录状态和Token信息。
     *
     * @param userInfo 用户信息
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<?> logout(@RequestBody UserInfo userInfo) {
        log.info("用户登出请求，用户ID: {}", userInfo.getId());
        return authService.logout(userInfo);
    }
}


