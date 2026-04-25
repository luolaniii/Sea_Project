package com.boot.study.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 基础Mapper接口
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作
 * 注意：由于MyBatis-Plus的BaseMapper已经存在，这里使用BaseMapperX避免冲突
 *
 * @param <T> 实体类型
 * @author study
 */
public interface BaseMapperX<T> extends BaseMapper<T> {
    // 可以在这里添加自定义的通用方法
}

