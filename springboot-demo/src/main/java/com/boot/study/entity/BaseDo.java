package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Map;
import java.time.LocalDateTime;

import com.boot.study.utils.JsonUtil;

/**
 * 基础实体类
 * 包含所有表的通用字段：创建时间、创建人、更新时间、更新人、删除时间、删除人、删除标记、扩展JSON
 *
 * @author study
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BaseDo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /**
     * 创建用户
     */
    @TableField(value = "created_user", fill = FieldFill.INSERT)
    private Long createdUser;

    /**
     * 修改时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    /**
     * 修改人ID
     */
    @TableField(value = "updated_user", fill = FieldFill.INSERT_UPDATE)
    private Long updatedUser;

    /**
     * 删除时间
     */
    @TableField(value = "deleted_time", fill = FieldFill.DEFAULT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deletedTime;

    /**
     * 删除人Id
     */
    @TableField(value = "deleted_user", fill = FieldFill.DEFAULT)
    private Long deletedUser;

    /**
     * 删除标记 0:可用 1:已删除
     */
    @TableLogic
    @TableField("deleted_flag")
    private Boolean deletedFlag;

    /**
     * 扩展信息（JSON格式）
     * 用于存储任何扩展信息，如标签、分类、优先级等
     */
    @TableField("ext_json")
    private String extJson;

    /**
     * 从ext_json中获取指定key的值
     *
     * @param key   键名
     * @param clazz 值类型
     * @param <V>   泛型类型
     * @return 值，如果不存在则返回null
     */
    public <V> V getExtValue(String key, Class<V> clazz) {
        return JsonUtil.getFromExtJson(this.getExtJson(), key, clazz);
    }

    /**
     * 向ext_json中添加或更新指定key的值
     *
     * @param key   键名
     * @param value 值
     */
    public void setExtValue(String key, Object value) {
        String extJsonValue = JsonUtil.putToExtJson(this.getExtJson(), key, value);
        this.setExtJson(extJsonValue);
    }

    /**
     * 从ext_json中移除指定key
     *
     * @param key 键名
     */
    public void removeExtValue(String key) {
        String extJsonValue = JsonUtil.removeFromExtJson(this.getExtJson(), key);
        this.setExtJson(extJsonValue);
    }

    /**
     * 检查ext_json是否包含指定key
     *
     * @param key 键名
     * @return 是否包含
     */
    public boolean containsExtKey(String key) {
        return JsonUtil.containsKey(this.getExtJson(), key);
    }

    /**
     * 获取ext_json的Map形式
     *
     * @return Map，如果ext_json为空则返回null
     */
    public Map<String, Object> getExtJsonMap() {
        return JsonUtil.parseMap(this.getExtJson());
    }

    /**
     * 设置ext_json（从Map）
     *
     * @param map Map对象
     */
    public void setExtJsonMap(Map<String, Object> map) {
        this.setExtJson(JsonUtil.toJsonString(map));
    }
}
