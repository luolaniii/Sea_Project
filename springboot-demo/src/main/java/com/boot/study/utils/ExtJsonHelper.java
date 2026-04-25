package com.boot.study.utils;

import com.alibaba.fastjson2.JSONObject;
import com.boot.study.entity.BaseDo;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * ExtJson辅助工具类
 * 提供便捷的方法操作BaseDo的ext_json字段
 *
 * @author study
 */
@Slf4j
public class ExtJsonHelper {

    /**
     * 常用扩展字段键名常量
     */
    public static class Keys {
        public static final String TAGS = "tags";
        public static final String CATEGORY = "category";
        public static final String PRIORITY = "priority";
        public static final String SORT_ORDER = "sortOrder";
        public static final String STATUS = "status";
        public static final String VERSION = "version";
        public static final String METADATA = "metadata";
        public static final String CUSTOM = "custom";
    }

    /**
     * 获取标签数组
     *
     * @param entity 实体对象
     * @param <T>    实体类型
     * @return 标签数组，如果不存在则返回null
     */
    public static <T extends BaseDo> String[] getTags(T entity) {
        return entity.getExtValue(Keys.TAGS, String[].class);
    }

    /**
     * 设置标签数组
     *
     * @param entity 实体对象
     * @param tags   标签数组
     * @param <T>    实体类型
     */
    public static <T extends BaseDo> void setTags(T entity, String[] tags) {
        entity.setExtValue(Keys.TAGS, tags);
    }

    /**
     * 获取分类
     *
     * @param entity 实体对象
     * @param <T>    实体类型
     * @return 分类，如果不存在则返回null
     */
    public static <T extends BaseDo> String getCategory(T entity) {
        return entity.getExtValue(Keys.CATEGORY, String.class);
    }

    /**
     * 设置分类
     *
     * @param entity   实体对象
     * @param category 分类
     * @param <T>      实体类型
     */
    public static <T extends BaseDo> void setCategory(T entity, String category) {
        entity.setExtValue(Keys.CATEGORY, category);
    }

    /**
     * 获取优先级
     *
     * @param entity 实体对象
     * @param <T>    实体类型
     * @return 优先级，如果不存在则返回null
     */
    public static <T extends BaseDo> Integer getPriority(T entity) {
        return entity.getExtValue(Keys.PRIORITY, Integer.class);
    }

    /**
     * 设置优先级
     *
     * @param entity   实体对象
     * @param priority 优先级
     * @param <T>      实体类型
     */
    public static <T extends BaseDo> void setPriority(T entity, Integer priority) {
        entity.setExtValue(Keys.PRIORITY, priority);
    }

    /**
     * 获取排序顺序
     *
     * @param entity 实体对象
     * @param <T>    实体类型
     * @return 排序顺序，如果不存在则返回null
     */
    public static <T extends BaseDo> Integer getSortOrder(T entity) {
        return entity.getExtValue(Keys.SORT_ORDER, Integer.class);
    }

    /**
     * 设置排序顺序
     *
     * @param entity    实体对象
     * @param sortOrder 排序顺序
     * @param <T>       实体类型
     */
    public static <T extends BaseDo> void setSortOrder(T entity, Integer sortOrder) {
        entity.setExtValue(Keys.SORT_ORDER, sortOrder);
    }

    /**
     * 获取版本号
     *
     * @param entity 实体对象
     * @param <T>    实体类型
     * @return 版本号，如果不存在则返回null
     */
    public static <T extends BaseDo> Integer getVersion(T entity) {
        return entity.getExtValue(Keys.VERSION, Integer.class);
    }

    /**
     * 设置版本号
     *
     * @param entity  实体对象
     * @param version 版本号
     * @param <T>     实体类型
     */
    public static <T extends BaseDo> void setVersion(T entity, Integer version) {
        entity.setExtValue(Keys.VERSION, version);
    }

    /**
     * 获取元数据
     *
     * @param entity 实体对象
     * @param <T>    实体类型
     * @return 元数据Map，如果不存在则返回null
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseDo> Map<String, Object> getMetadata(T entity) {
        return (Map<String, Object>) entity.getExtValue(Keys.METADATA, Map.class);
    }

    /**
     * 设置元数据
     *
     * @param entity   实体对象
     * @param metadata 元数据Map
     * @param <T>      实体类型
     */
    public static <T extends BaseDo> void setMetadata(T entity, Map<String, Object> metadata) {
        entity.setExtValue(Keys.METADATA, metadata);
    }

    /**
     * 获取自定义字段
     *
     * @param entity 实体对象
     * @param <T>    实体类型
     * @return 自定义字段Map，如果不存在则返回null
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseDo> Map<String, Object> getCustom(T entity) {
        return (Map<String, Object>) entity.getExtValue(Keys.CUSTOM, Map.class);
    }

    /**
     * 设置自定义字段
     *
     * @param entity 实体对象
     * @param custom 自定义字段Map
     * @param <T>    实体类型
     */
    public static <T extends BaseDo> void setCustom(T entity, Map<String, Object> custom) {
        entity.setExtValue(Keys.CUSTOM, custom);
    }

    /**
     * 初始化ext_json（如果为空）
     *
     * @param entity 实体对象
     * @param <T>    实体类型
     */
    public static <T extends BaseDo> void initExtJson(T entity) {
        if (entity.getExtJson() == null || entity.getExtJson().trim().isEmpty()) {
            entity.setExtJson("{}");
        }
    }

    /**
     * 合并ext_json
     *
     * @param entity     实体对象
     * @param extJsonMap 要合并的Map
     * @param <T>        实体类型
     */
    public static <T extends BaseDo> void mergeExtJson(T entity, Map<String, Object> extJsonMap) {
        if (extJsonMap == null || extJsonMap.isEmpty()) {
            return;
        }
        initExtJson(entity);
        JSONObject jsonObject = JsonUtil.parseObject(entity.getExtJson());
        if (jsonObject == null) {
            jsonObject = new JSONObject();
        }
        jsonObject.putAll(extJsonMap);
        entity.setExtJson(jsonObject.toJSONString());
    }
}

