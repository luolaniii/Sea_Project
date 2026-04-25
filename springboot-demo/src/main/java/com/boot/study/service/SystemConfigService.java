package com.boot.study.service;

import com.boot.study.api.req.admin.SystemConfigPageReq;
import com.boot.study.entity.SystemConfig;
import com.boot.study.entity.SystemConfigVO;
import com.boot.study.response.PageBean;

/**
 * 系统配置表 Service
 *
 * @author study
 */
public interface SystemConfigService extends BaseService<SystemConfig> {

    /**
     * 分页查询系统配置
     *
     * @param req 查询请求
     * @return 分页结果
     */
    PageBean<SystemConfig> pageList(SystemConfigPageReq req);

    /**
     * 分页查询系统配置（包含枚举描述）
     *
     * @param req 查询请求
     * @return 分页结果（VO对象，包含枚举描述）
     */
    PageBean<SystemConfigVO> pageListWithVO(SystemConfigPageReq req);
}


