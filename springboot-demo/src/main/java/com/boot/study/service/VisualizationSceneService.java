package com.boot.study.service;

import com.boot.study.api.req.admin.VisualizationScenePageReq;
import com.boot.study.api.req.user.SceneDataQueryReq;
import com.boot.study.api.req.user.UserScenePageReq;
import com.boot.study.entity.ObservationDataVO;
import com.boot.study.entity.VisualizationScene;
import com.boot.study.entity.VisualizationSceneVO;
import com.boot.study.response.PageBean;

/**
 * 可视化场景表 Service
 *
 * @author study
 */
public interface VisualizationSceneService extends BaseService<VisualizationScene> {

    /**
     * 分页查询可视化场景
     *
     * @param req 查询请求
     * @return 分页结果
     */
    PageBean<VisualizationScene> pageList(VisualizationScenePageReq req);

    /**
     * 分页查询可视化场景（包含枚举描述）
     *
     * @param req 查询请求
     * @return 分页结果（VO对象，包含枚举描述）
     */
    PageBean<VisualizationSceneVO> pageListWithVO(VisualizationScenePageReq req);

    /**
     * 用户端 - 分页查询公开的场景配置
     * <p>
     * 只查询公开且未删除的场景配置，支持按场景名称、场景类型筛选
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果（VO对象，包含枚举描述字段）
     */
    PageBean<VisualizationSceneVO> getPublicScenes(UserScenePageReq req);

    /**
     * 用户端 - 根据ID获取公开的场景详情
     * <p>
     * 验证场景是否存在、是否公开、是否已删除，并增加查看次数
     *
     * @param id 场景ID
     * @return 场景配置VO对象
     * @throws com.boot.study.exception.ServiceException 如果场景不存在或无权访问
     */
    VisualizationSceneVO getPublicSceneById(Long id);

    /**
     * 用户端 - 查询场景关联的观测数据
     * <p>
     * 验证场景是否存在且公开，然后查询关联的观测数据，支持经纬度范围过滤
     *
     * @param sceneId 场景ID
     * @param req 数据查询请求
     * @return 观测数据分页结果
     * @throws com.boot.study.exception.ServiceException 如果场景不存在或无权访问
     */
    PageBean<ObservationDataVO> getSceneData(Long sceneId, SceneDataQueryReq req);
}


