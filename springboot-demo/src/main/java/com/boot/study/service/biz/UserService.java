package com.boot.study.service.biz;

import com.boot.study.api.req.RegisterReq;
import com.boot.study.bean.Result;

public interface UserService {
    /**
     * 用户注册
     *
     * @param req 注册参数
     *            默认注册为普通用户角色：user，状态为启用
     * @return Result<?>
     */
    Result<?> register(RegisterReq req);
}
