package com.boot.study.service.biz;

import com.boot.study.api.req.LoginReq;
import com.boot.study.bean.Result;
import com.boot.study.bean.UserInfo;

/**
 * 认证相关业务 Service接口
 * <p>
 * 提供用户登录、登出等认证相关的业务逻辑接口
 *
 * @author study
 * @since 2024
 */
public interface AuthService {

    /**
     * 用户登录
     * <p>
     * 验证用户名和密码，登录成功后生成JWT Token并缓存用户信息
     *
     * @param req 登录请求，包含用户名和密码
     * @return 登录结果（包含用户信息和Token）
     */
    Result<UserInfo> login(LoginReq req);

    /**
     * 用户登出
     * <p>
     * 清除Redis中缓存的用户Token信息
     *
     * @param userInfo 用户信息，包含Token和用户ID
     * @return 处理结果
     */
    Result<?> logout(UserInfo userInfo);
}


