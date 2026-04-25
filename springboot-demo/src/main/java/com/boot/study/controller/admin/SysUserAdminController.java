package com.boot.study.controller.admin;

import com.boot.study.controller.BaseController;
import com.boot.study.entity.SysUser;
import com.boot.study.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 用户管理接口
 *
 * 复用通用 BaseController，提供基础 CRUD 能力
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sys-user")
public class SysUserAdminController extends BaseController<SysUserService, SysUser> {
}


