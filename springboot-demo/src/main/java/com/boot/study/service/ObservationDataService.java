package com.boot.study.service;

import com.boot.study.api.req.admin.ObservationDataPageReq;
import com.boot.study.entity.ObservationData;
import com.boot.study.entity.ObservationDataVO;
import com.boot.study.response.PageBean;

/**
 * 观测数据表 Service
 *
 * @author study
 */
public interface ObservationDataService extends BaseService<ObservationData> {

    /**
     * 分页查询观测数据
     *
     * @param req 查询请求
     * @return 分页结果
     */
    PageBean<ObservationData> pageList(ObservationDataPageReq req);

    /**
     * 分页查询观测数据（包含关联信息）
     *
     * @param req 查询请求
     * @return 分页结果（VO对象，包含数据源和数据类型信息）
     */
    PageBean<ObservationDataVO> pageListWithRelation(ObservationDataPageReq req);
}


