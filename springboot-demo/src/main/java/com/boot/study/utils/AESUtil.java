package com.boot.study.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;

public class AESUtil {

    /**
     * 加密秘钥
     */
    private final static String SECRET = "crKPfREEaugcAkPI";

    private static final AES AES = SecureUtil.aes(SECRET.getBytes(StandardCharsets.UTF_8));

    /**
     * 加密
     */
    public static String encrypt(String string) {
        return AES.encryptBase64(string);
    }

    /**
     * 解密
     */
    public static String decrypt(String string) {
        return AES.decryptStr(string);
    }
}
