package com.boot.study.service;

import com.boot.study.api.req.admin.DataSourcePageReq;
import com.boot.study.entity.DataSource;
import com.boot.study.entity.DataSourceVO;
import com.boot.study.response.PageBean;

/**
 * 数据源表 Service
 *
 * @author study
 */
public interface DataSourceService extends BaseService<DataSource> {

    /**
     * 分页查询数据源
     *
     * @param req 查询请求
     * @return 分页结果
     */
    PageBean<DataSource> pageList(DataSourcePageReq req);

    /**
     * 分页查询数据源（包含枚举描述）
     *
     * @param req 查询请求
     * @return 分页结果（VO对象，包含枚举描述）
     */
    PageBean<DataSourceVO> pageListWithVO(DataSourcePageReq req);
}


