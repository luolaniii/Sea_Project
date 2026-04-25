package com.boot.study.utils;

import cn.hutool.core.util.StrUtil;
import com.boot.study.exception.ServiceException;

/**
 * 登录工具类
 * <p>
 * 提供登录相关的工具方法，包括密码错误次数验证和账号锁定功能。
 * 使用Redis存储密码错误次数，达到5次后锁定账号1小时。
 *
 * @author study
 * @since 2024
 */
public class LoginUtil {

    /**
     * Redis中存储密码错误次数的key前缀
     */
    private static final String ACCOUNT_SECURITY_ERROR = "ACCOUNT_SECURITY_ERROR:";

    /**
     * 最大允许的密码错误次数
     */
    private static final int MAX_ERROR_COUNT = 5;

    /**
     * 账号锁定时间（秒）
     */
    private static final long LOCK_TIME_SECONDS = 60 * 60;

    /**
     * 密码错误次数验证
     * <p>
     * 功能说明：
     * 1. 检查当前账号是否已被锁定
     * 2. 如果密码错误，增加错误次数计数
     * 3. 如果错误次数达到5次，锁定账号1小时
     * 4. 如果密码正确，清除错误次数计数
     *
     * @param loginPassword 前端传入的密码
     * @param userPassword  数据库中存储的密码
     * @param userId        用户ID
     * @throws ServiceException 当账号被锁定或密码错误时抛出异常
     */
    public static void loginCount(String loginPassword, String userPassword, Long userId) {
        String key = ACCOUNT_SECURITY_ERROR + userId;

        // 获取当前错误次数
        Integer errorCount = RedisUtil.getInt(key);

        // 如果错误次数已达到上限，检查锁定剩余时间
        if (errorCount != null && errorCount >= MAX_ERROR_COUNT) {
            Long remainingTime = RedisUtil.getExpire(key);
            throw new ServiceException(StrUtil.format("账号锁定中，剩余解锁时间:{}秒", remainingTime));
        }

        // 验证密码
        if (!userPassword.equals(loginPassword)) {
            // 密码错误，增加错误次数，并设置过期时间为1小时
            Long newErrorCount = RedisUtil.incrExpire(key, LOCK_TIME_SECONDS);
            throw new ServiceException(StrUtil.format("密码错误，错误次数:{}，五次错误后锁定账号一小时！", newErrorCount));
        }

        // 登录成功，清除错误次数记录
        RedisUtil.del(key);
    }
}
