package com.boot.study.service;

import com.boot.study.entity.SysUser;

/**
 * 用户表 Service
 */
public interface SysUserService extends BaseService<SysUser> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户
     */
    SysUser getByUsername(String username);
}


