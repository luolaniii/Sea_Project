package com.boot.study.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.boot.study.bean.TokenInfo;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;

import java.nio.charset.StandardCharsets;

/**
 * JWT Token工具类
 * <p>
 * 提供JWT Token的创建和解析功能，用于用户认证和授权。
 * 使用Hutool的JWT工具类实现。
 *
 * @author study
 * @since 2024
 */
public class TokenUtil {

    /**
     * JWT签名密钥
     * <p>
     * 注意：生产环境应该从配置文件或环境变量中读取，不要硬编码
     */
    private static final String SIGNING_KEY = "qAWMbVKdkAcVaVuY";

    /**
     * 创建JWT Token
     * <p>
     * 根据用户ID和角色类型生成JWT Token，Token中包含：
     * - userId: 用户ID
     * - roleType: 角色类型（如：ADMIN、USER）
     * - iat: 签发时间
     *
     * @param userId  用户ID
     * @param roleType 角色类型
     * @return TokenInfo对象，包含token和tokenPath
     */
    public static TokenInfo createJWT(Long userId, String roleType) {
        String sign = JWT.create()
                .setIssuedAt(DateUtil.date())
                .setKey(SIGNING_KEY.getBytes(StandardCharsets.UTF_8))
                .setHeader("userId", userId)
                .setHeader("roleType", roleType)
                .sign();
        return TokenInfo.builder()
                .token(sign)
                .tokenPath(roleType + ":" + userId)
                .build();
    }

    /**
     * 解析JWT Token
     * <p>
     * 验证Token的有效性，并提取其中的用户信息。
     * 如果Token无效或已过期，会抛出ServiceException异常。
     *
     * @param token JWT Token字符串
     * @return TokenInfo对象，包含tokenPath（格式：roleType:userId）
     * @throws ServiceException 当Token验证失败时抛出
     */
    public static TokenInfo parse(String token) {
        // 验证Token签名和有效性
        if (!JWTUtil.verify(token, SIGNING_KEY.getBytes(StandardCharsets.UTF_8))) {
            throw new ServiceException(ResultEnum.TOKEN_DECODE_ERROR);
        }
        // 解析Token，提取用户信息
        JWT jwt = JWTUtil.parseToken(token);
        Object userId = jwt.getHeader("userId");
        Object roleType = jwt.getHeader("roleType");
        return TokenInfo.builder()
                .tokenPath(roleType + ":" + userId)
                .build();
    }
}
