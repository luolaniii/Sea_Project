package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boot.study.dao.BaseMapperX;
import com.boot.study.service.BaseService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;

/**
 * 基础Service实现类
 * 实现BaseService接口，提供基础的CRUD操作实现
 *
 * @param <M> Mapper类型
 * @param <T> 实体类型
 * @author study
 */
@Slf4j
public class BaseServiceImpl<M extends BaseMapperX<T>, T> extends ServiceImpl<M, T> implements BaseService<T> {

    /**
     * 根据ID查询（忽略逻辑删除）
     * <p>
     * 注意：MyBatis-Plus的逻辑删除是自动处理的，如果需要真正忽略逻辑删除，
     * 需要在Mapper中自定义SQL。这里提供一个基础实现，子类可以重写此方法。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    @Override
    public T getByIdIgnoreDeleted(Long id) {
        log.warn("getByIdIgnoreDeleted方法需要自定义Mapper实现，当前使用默认实现（不忽略逻辑删除）");
        return getById(id);
    }

    /**
     * 根据条件查询列表（忽略逻辑删除）
     * <p>
     * 注意：MyBatis-Plus的逻辑删除是自动处理的，如果需要真正忽略逻辑删除，
     * 需要在Mapper中自定义SQL。这里提供一个基础实现，子类可以重写此方法。
     *
     * @param queryWrapper 查询条件
     * @return 实体列表
     */
    @Override
    public List<T> listIgnoreDeleted(Wrapper<T> queryWrapper) {
        log.warn("listIgnoreDeleted方法需要自定义Mapper实现，当前使用默认实现（不忽略逻辑删除）");
        return list(queryWrapper);
    }

    /**
     * 分页查询（忽略逻辑删除）
     * <p>
     * 注意：MyBatis-Plus的逻辑删除是自动处理的，如果需要真正忽略逻辑删除，
     * 需要在Mapper中自定义SQL。这里提供一个基础实现，子类可以重写此方法。
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<T> pageIgnoreDeleted(Page<T> page, Wrapper<T> queryWrapper) {
        log.warn("pageIgnoreDeleted方法需要自定义Mapper实现，当前使用默认实现（不忽略逻辑删除）");
        return page(page, queryWrapper);
    }

    /**
     * 批量保存（忽略重复）
     * <p>
     * 批量保存时忽略重复记录，如果因为重复键失败，会尝试逐条保存。
     * 这是一个基础实现，子类可以根据具体业务需求重写此方法。
     *
     * @param entityList 实体列表
     * @return 是否成功
     */
    @Override
    public boolean saveBatchIgnoreDuplicate(List<T> entityList) {
        try {
            return saveBatch(entityList);
        } catch (Exception e) {
            log.warn("批量保存失败，尝试逐条保存: {}", e.getMessage());
            // 如果是因为重复键失败，尝试逐条保存
            boolean allSuccess = true;
            for (T entity : entityList) {
                try {
                    save(entity);
                } catch (Exception ex) {
                    log.warn("保存实体失败，可能已存在: {}", entity, ex);
                    allSuccess = false;
                }
            }
            return allSuccess;
        }
    }

    /**
     * 根据ID物理删除（不进行逻辑删除）
     * <p>
     * 注意：这会真正删除数据库中的数据，请谨慎使用。
     * 建议优先使用逻辑删除（removeById）。
     *
     * @param id 主键ID
     * @return 是否成功
     */
    @Override
    public boolean removeByIdPhysical(Long id) {
        log.warn("执行物理删除操作，ID: {}", id);
        return removeById(id, false);
    }

    /**
     * 根据ID批量物理删除（不进行逻辑删除）
     * <p>
     * 注意：这会真正删除数据库中的数据，请谨慎使用。
     * 建议优先使用逻辑删除（removeByIds）。
     *
     * @param idList 主键ID列表
     * @return 是否成功
     */
    @Override
    public boolean removeByIdsPhysical(Collection<? extends Long> idList) {
        log.warn("执行批量物理删除操作，数量: {}", idList != null ? idList.size() : 0);
        return removeByIds(idList, false);
    }
}

