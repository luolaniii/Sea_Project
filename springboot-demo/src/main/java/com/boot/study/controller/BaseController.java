package com.boot.study.controller;

import com.boot.study.bean.Result;
import com.boot.study.entity.BaseDo;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.BaseService;
import com.boot.study.utils.PathLongParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 基础Controller抽象类
 * <p>
 * 提供通用的CRUD接口，子类继承此类即可获得基础的增删改查功能。
 * 使用泛型设计，支持任意实体类型和对应的Service类型。
 *
 * @param <S> Service类型，必须继承BaseService
 * @param <T> 实体类型，必须继承BaseDo
 * @author study
 * @since 2024
 */
@Slf4j
public abstract class BaseController<S extends BaseService<T>, T extends BaseDo> {

    /**
     * 基础Service，由子类注入具体的Service实现
     */
    @Autowired
    protected S baseService;

    /**
     * 根据ID查询实体
     *
     * @param id 主键ID
     * @return 查询结果，包含实体对象
     */
    @GetMapping("/{id}")
    public Result<T> getById(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        T entity = baseService.getById(id);
        return Result.success(entity);
    }

    /**
     * 保存实体
     * <p>
     * 新增操作，保存新的实体到数据库
     *
     * @param entity 实体对象
     * @return 保存结果，包含保存后的实体对象
     */
    @PostMapping
    public Result<T> save(@RequestBody T entity) {
        boolean success = baseService.save(entity);
        if (!success) {
            throw new ServiceException(ResultEnum.SAVE_FAILED);
        }
        return Result.success(entity);
    }

    /**
     * 更新实体
     * <p>
     * 更新操作，根据ID更新实体信息
     *
     * @param entity 实体对象（必须包含ID）
     * @return 更新结果，包含更新后的实体对象
     */
    @PutMapping
    public Result<T> update(@RequestBody T entity) {
        if (entity.getId() == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "ID不能为空");
        }
        boolean success = baseService.updateById(entity);
        if (!success) {
            throw new ServiceException(ResultEnum.UPDATE_FAILED);
        }
        return Result.success(entity);
    }

    /**
     * 根据ID删除实体（逻辑删除）
     * <p>
     * 执行逻辑删除，不会真正删除数据库记录，只是标记为已删除
     *
     * @param id 主键ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        boolean success = baseService.removeById(id);
        if (!success) {
            throw new ServiceException(ResultEnum.DELETE_FAILED);
        }
        return Result.success();
    }

    /**
     * 批量删除实体（逻辑删除）
     * <p>
     * 根据ID列表批量执行逻辑删除
     *
     * @param ids ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public Result<?> deleteBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "ID列表不能为空");
        }
        boolean success = baseService.removeByIds(ids);
        if (!success) {
            throw new ServiceException(ResultEnum.DELETE_FAILED);
        }
        return Result.success();
    }

    /**
     * 批量保存实体
     * <p>
     * 批量新增操作，一次性保存多个实体到数据库
     *
     * @param entityList 实体列表
     * @return 保存结果，包含保存后的实体列表
     */
    @PostMapping("/batch")
    public Result<List<T>> saveBatch(@RequestBody List<T> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "实体列表不能为空");
        }
        boolean success = baseService.saveBatch(entityList);
        if (!success) {
            throw new ServiceException(ResultEnum.SAVE_FAILED);
        }
        return Result.success(entityList);
    }
}

