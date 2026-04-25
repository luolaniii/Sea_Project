package com.boot.study.service;

import com.boot.study.api.req.admin.ChartConfigPageReq;
import com.boot.study.api.req.user.ChartDataQueryReq;
import com.boot.study.api.req.user.UserChartPageReq;
import com.boot.study.entity.ChartConfig;
import com.boot.study.entity.ChartConfigVO;
import com.boot.study.entity.ObservationDataVO;
import com.boot.study.response.PageBean;

/**
 * 图表配置表 Service接口
 * <p>
 * 提供图表配置相关的业务逻辑接口，包括分页查询等功能
 *
 * @author study
 * @since 2024
 */
public interface ChartConfigService extends BaseService<ChartConfig> {

    /**
     * 分页查询图表配置
     * <p>
     * 根据查询条件分页查询图表配置列表，支持按图表名称、图表类型、是否公开等条件筛选
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果，包含图表配置列表
     */
    PageBean<ChartConfig> pageList(ChartConfigPageReq req);

    /**
     * 分页查询图表配置（包含枚举描述）
     * <p>
     * 分页查询图表配置，并将枚举类型转换为包含描述信息的VO对象
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果（VO对象，包含枚举描述字段）
     */
    PageBean<ChartConfigVO> pageListWithVO(ChartConfigPageReq req);

    /**
     * 用户端 - 分页查询公开的图表配置
     * <p>
     * 只查询公开且未删除的图表配置，支持按图表名称、图表类型筛选
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果（VO对象，包含枚举描述字段）
     */
    PageBean<ChartConfigVO> getPublicCharts(UserChartPageReq req);

    /**
     * 用户端 - 根据ID获取公开的图表详情
     * <p>
     * 验证图表是否存在、是否公开、是否已删除
     *
     * @param id 图表ID
     * @return 图表配置VO对象
     * @throws com.boot.study.exception.ServiceException 如果图表不存在或无权访问
     */
    ChartConfigVO getPublicChartById(Long id);

    /**
     * 用户端 - 查询图表关联的观测数据
     * <p>
     * 验证图表是否存在且公开，然后查询关联的观测数据，支持经纬度范围过滤
     *
     * @param chartId 图表ID
     * @param req 数据查询请求
     * @return 观测数据分页结果
     * @throws com.boot.study.exception.ServiceException 如果图表不存在或无权访问
     */
    PageBean<ObservationDataVO> getChartData(Long chartId, ChartDataQueryReq req);
}


