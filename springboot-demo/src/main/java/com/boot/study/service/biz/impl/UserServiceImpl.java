package com.boot.study.service.biz.impl;

import com.boot.study.api.req.RegisterReq;
import com.boot.study.bean.Result;
import com.boot.study.entity.SysUser;
import com.boot.study.enums.ResultEnum;
import com.boot.study.enums.StatusEnum;
import com.boot.study.enums.UserRoleEnum;
import com.boot.study.service.SysUserService;
import com.boot.study.service.biz.UserService;
import com.boot.study.utils.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final SysUserService sysUserService;

    @Override
    public Result<?> register(RegisterReq req) {
        if (!StringUtils.hasText(req.getUsername()) || !StringUtils.hasText(req.getPassword())) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "用户名和密码不能为空");
        }

        // 检查用户名是否已存在
        SysUser exists = sysUserService.getByUsername(req.getUsername());
        if (exists != null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(AESUtil.encrypt(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setRealName(req.getRealName());
        user.setRole(UserRoleEnum.USER.getCode());
        user.setStatus(StatusEnum.ENABLED.getCode());

        boolean saved = sysUserService.save(user);
        if (!saved) {
            return Result.fail(ResultEnum.SAVE_FAILED);
        }
        return Result.success();
    }

}
