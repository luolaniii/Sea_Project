package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户表实体
 * <p>
 * 对应数据库表：sys_user
 * 用于存储系统用户的基本信息，包括用户名、密码、角色、状态等
 *
 * @author study
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseDo {

    /**
     * 用户名
     */
    private String username;

    /**
     * 加密后的密码
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 角色：admin-管理员, user-普通用户, expert-专家
     */
    private String role;

    /**
     * 专家头衔
     */
    private String expertTitle;

    /**
     * 专家领域
     */
    private String expertField;

    /**
     * 状态：0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 备注
     */
    private String remark;
}


