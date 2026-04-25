package com.boot.study.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * JSON工具类
 * 用于处理ext_json字段的序列化和反序列化
 *
 * @author study
 */
@Slf4j
public class JsonUtil {

    /**
     * 将对象转换为JSON字符串
     *
     * @param obj 对象
     * @return JSON字符串，如果对象为null则返回null
     */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            log.error("对象转JSON字符串失败", e);
            return null;
        }
    }

    /**
     * 将JSON字符串转换为指定类型的对象
     *
     * @param jsonStr JSON字符串
     * @param clazz   目标类型
     * @param <T>     泛型类型
     * @return 转换后的对象，如果JSON字符串为null或空则返回null
     */
    public static <T> T parseObject(String jsonStr, Class<T> clazz) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(jsonStr, clazz);
        } catch (Exception e) {
            log.error("JSON字符串转对象失败: {}", jsonStr, e);
            return null;
        }
    }

    /**
     * 将JSON字符串转换为JSONObject
     *
     * @param jsonStr JSON字符串
     * @return JSONObject，如果JSON字符串为null或空则返回null
     */
    public static JSONObject parseObject(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(jsonStr);
        } catch (Exception e) {
            log.error("JSON字符串转JSONObject失败: {}", jsonStr, e);
            return null;
        }
    }

    /**
     * 将JSON字符串转换为Map
     *
     * @param jsonStr JSON字符串
     * @return Map，如果JSON字符串为null或空则返回null
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseMap(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return null;
        }
        try {
            return (Map<String, Object>) JSON.parseObject(jsonStr, Map.class);
        } catch (Exception e) {
            log.error("JSON字符串转Map失败: {}", jsonStr, e);
            return null;
        }
    }

    /**
     * 从ext_json中获取指定key的值
     *
     * @param extJson ext_json字符串
     * @param key     键名
     * @param clazz   值类型
     * @param <T>     泛型类型
     * @return 值，如果不存在则返回null
     */
    public static <T> T getFromExtJson(String extJson, String key, Class<T> clazz) {
        JSONObject jsonObject = parseObject(extJson);
        if (jsonObject == null) {
            return null;
        }
        try {
            return jsonObject.getObject(key, clazz);
        } catch (Exception e) {
            log.error("从ext_json获取值失败, key: {}", key, e);
            return null;
        }
    }

    /**
     * 向ext_json中添加或更新指定key的值
     *
     * @param extJson 原始ext_json字符串
     * @param key     键名
     * @param value   值
     * @return 更新后的JSON字符串
     */
    public static String putToExtJson(String extJson, String key, Object value) {
        JSONObject jsonObject = parseObject(extJson);
        if (jsonObject == null) {
            jsonObject = new JSONObject();
        }
        jsonObject.put(key, value);
        return jsonObject.toJSONString();
    }

    /**
     * 从ext_json中移除指定key
     *
     * @param extJson 原始ext_json字符串
     * @param key     键名
     * @return 更新后的JSON字符串
     */
    public static String removeFromExtJson(String extJson, String key) {
        JSONObject jsonObject = parseObject(extJson);
        if (jsonObject == null) {
            return null;
        }
        jsonObject.remove(key);
        return jsonObject.isEmpty() ? null : jsonObject.toJSONString();
    }

    /**
     * 检查ext_json是否包含指定key
     *
     * @param extJson ext_json字符串
     * @param key     键名
     * @return 是否包含
     */
    public static boolean containsKey(String extJson, String key) {
        JSONObject jsonObject = parseObject(extJson);
        return jsonObject != null && jsonObject.containsKey(key);
    }

    /**
     * 验证JSON字符串是否有效
     *
     * @param jsonStr JSON字符串
     * @return 是否有效
     */
    public static boolean isValidJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return false;
        }
        try {
            JSON.parse(jsonStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

