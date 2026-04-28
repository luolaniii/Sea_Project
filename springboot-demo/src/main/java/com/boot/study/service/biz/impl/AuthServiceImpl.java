package com.boot.study.service.biz.impl;

import com.boot.study.api.req.LoginReq;
import com.boot.study.bean.Result;
import com.boot.study.bean.TokenInfo;
import com.boot.study.bean.UserInfo;
import com.boot.study.consts.BaseConst;
import com.boot.study.entity.SysUser;
import com.boot.study.enums.ResultEnum;
import com.boot.study.service.SysUserService;
import com.boot.study.service.biz.AuthService;
import com.boot.study.utils.AESUtil;
import com.boot.study.utils.LoginUtil;
import com.boot.study.utils.RedisUtil;
import com.boot.study.utils.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证相关业务 Service 实现类
 * <p>
 * 实现用户登录、登出等认证相关的业务逻辑
 *
 * @author study
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /**
     * 系统用户Service
     */
    private final SysUserService sysUserService;

    /**
     * 用户登录
     * <p>
     * 验证用户名和密码，登录成功后生成JWT Token并缓存用户信息
     *
     * @param req 登录请求，包含用户名和密码
     * @return 登录结果，包含用户信息和Token
     */
    @Override
    public Result<UserInfo> login(LoginReq req) {
        // 根据用户名查询用户
        SysUser user = sysUserService.getByUsername(req.getUsername());
        if (user == null) {
            return Result.fail(ResultEnum.PWD_ERROR);
        }

        // 前端传明文，DB存加密密码，这里先加密再比对
        String encryptPwd = AESUtil.encrypt(req.getPassword());
        // 验证密码并记录登录次数
        LoginUtil.loginCount(encryptPwd, user.getPassword(), user.getId());

        // 生成JWT Token
        TokenInfo tokenInfo = TokenUtil.createJWT(user.getId(), user.getRole());
        
        // 构建用户信息对象
        UserInfo userInfo = UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .token(tokenInfo.getToken())
                .avatar(user.getAvatar())
                .build();
        
        // 缓存 token -> UserInfo，使用全局前缀
        RedisUtil.set(BaseConst.TOKEN + tokenInfo.getTokenPath(), userInfo, BaseConst.EXPIRATION_TIME);

        return Result.success(userInfo);
    }

    /**
     * 用户登出
     * <p>
     * 清除Redis中缓存的用户Token信息
     *
     * @param userInfo 用户信息，包含Token和用户ID
     * @return 登出结果
     */
    @Override
    public Result<?> logout(UserInfo userInfo) {
        if (userInfo != null && userInfo.getToken() != null 
                && userInfo.getRole() != null && userInfo.getId() != null) {
            // 构建Token路径并删除缓存
            String tokenPath = userInfo.getRole() + ":" + userInfo.getId();
            RedisUtil.del(BaseConst.TOKEN + tokenPath);
        }
        return Result.success(ResultEnum.HANDLE_SUCCESS);
    }
}


