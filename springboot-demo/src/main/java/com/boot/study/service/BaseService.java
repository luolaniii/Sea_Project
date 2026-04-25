package com.boot.study.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;

/**
 * 基础Service接口
 * 继承MyBatis-Plus的IService，提供基础的CRUD操作
 *
 * @param <T> 实体类型
 * @author study
 */
public interface BaseService<T> extends IService<T> {

    /**
     * 根据ID查询（忽略逻辑删除）
     *
     * @param id 主键ID
     * @return 实体对象
     */
    T getByIdIgnoreDeleted(Long id);

    /**
     * 根据条件查询列表（忽略逻辑删除）
     *
     * @param queryWrapper 查询条件
     * @return 实体列表
     */
    List<T> listIgnoreDeleted(Wrapper<T> queryWrapper);

    /**
     * 分页查询（忽略逻辑删除）
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    IPage<T> pageIgnoreDeleted(Page<T> page, Wrapper<T> queryWrapper);

    /**
     * 批量保存（忽略重复）
     *
     * @param entityList 实体列表
     * @return 是否成功
     */
    boolean saveBatchIgnoreDuplicate(List<T> entityList);

    /**
     * 根据ID物理删除（不进行逻辑删除）
     *
     * @param id 主键ID
     * @return 是否成功
     */
    boolean removeByIdPhysical(Long id);

    /**
     * 根据ID批量物理删除（不进行逻辑删除）
     *
     * @param idList 主键ID列表
     * @return 是否成功
     */
    boolean removeByIdsPhysical(Collection<? extends Long> idList);
}

