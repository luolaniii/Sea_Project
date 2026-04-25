package com.boot.study.controller.common;

import com.boot.study.annotation.PassToken;
import com.boot.study.api.req.RegisterReq;
import com.boot.study.bean.Result;
import com.boot.study.service.biz.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用 - 用户相关接口
 * <p>
 * 提供用户注册等通用功能接口，无需Token认证
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/common/user")
@RequiredArgsConstructor
@PassToken
public class UserController {

    /**
     * 用户业务Service
     */
    private final UserService userService;

    /**
     * 用户注册
     * <p>
     * 注册新用户，默认注册为普通用户角色（user），状态为启用
     *
     * @param req 注册请求，包含用户名、密码等信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterReq req) {
        return userService.register(req);
    }
}


