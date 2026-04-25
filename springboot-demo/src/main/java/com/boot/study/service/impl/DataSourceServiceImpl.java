package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.study.api.req.admin.DataSourcePageReq;
import com.boot.study.dao.DataSourceMapper;
import com.boot.study.entity.DataSource;
import com.boot.study.entity.DataSourceVO;
import com.boot.study.response.PageBean;
import com.boot.study.service.DataSourceService;
import com.boot.study.utils.EnumConvertUtil;
import com.boot.study.utils.PageHelperUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据源表 Service 实现类
 * <p>
 * 实现数据源相关的业务逻辑，包括分页查询、数据转换等功能
 *
 * @author study
 * @since 2024
 */
@Slf4j
@Service
public class DataSourceServiceImpl extends BaseServiceImpl<DataSourceMapper, DataSource> implements DataSourceService {

    /**
     * 分页查询数据源
     * <p>
     * 根据查询条件构建查询条件，支持按数据源名称模糊查询、数据源类型精确查询、
     * 状态精确查询，并按创建时间倒序排列
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果，包含数据源列表
     */
    @Override
    public PageBean<DataSource> pageList(DataSourcePageReq req) {
        LambdaQueryWrapper<DataSource> wrapper = Wrappers.lambdaQuery();
        
        // 数据源名称模糊查询
        if (StringUtils.hasText(req.getSourceName())) {
            wrapper.like(DataSource::getSourceName, req.getSourceName());
        }
        
        // 数据源类型精确查询
        if (StringUtils.hasText(req.getSourceType())) {
            wrapper.eq(DataSource::getSourceType, req.getSourceType());
        }
        
        // 状态精确查询
        if (req.getStatus() != null) {
            wrapper.eq(DataSource::getStatus, req.getStatus());
        }
        
        // 按创建时间倒序排列
        wrapper.orderByDesc(DataSource::getCreatedTime);

        Page<DataSource> page = this.page(PageHelperUtil.page(req), wrapper);
        return PageBean.page(page, page.getRecords());
    }

    /**
     * 分页查询数据源（包含枚举描述）
     * <p>
     * 先调用分页查询方法获取数据，然后将实体对象转换为包含枚举描述的VO对象
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果（VO对象，包含枚举描述字段）
     */
    @Override
    public PageBean<DataSourceVO> pageListWithVO(DataSourcePageReq req) {
        // 先获取分页数据
        PageBean<DataSource> pageBean = pageList(req);
        
        // 将实体对象转换为VO对象（包含枚举描述）
        List<DataSourceVO> voList = pageBean.getList().stream()
                .map(EnumConvertUtil::convertDataSource)
                .collect(Collectors.toList());

        // 构建VO分页结果
        PageBean<DataSourceVO> voPageBean = new PageBean<>();
        voPageBean.setPageNum(pageBean.getPageNum());
        voPageBean.setPageSize(pageBean.getPageSize());
        voPageBean.setSize(pageBean.getSize());
        voPageBean.setTotal(pageBean.getTotal());
        voPageBean.setPages(pageBean.getPages());
        voPageBean.setList(voList);
        return voPageBean;
    }
}


