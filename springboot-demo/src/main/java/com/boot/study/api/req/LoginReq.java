package com.boot.study.api.req;

import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginReq {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（明文，后端加密校验）
     */
    private String password;
}


