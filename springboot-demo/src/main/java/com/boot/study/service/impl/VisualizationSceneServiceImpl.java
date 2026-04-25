package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.study.api.req.admin.ObservationDataPageReq;
import com.boot.study.api.req.admin.VisualizationScenePageReq;
import com.boot.study.api.req.user.SceneDataQueryReq;
import com.boot.study.api.req.user.UserScenePageReq;
import com.boot.study.dao.VisualizationSceneMapper;
import com.boot.study.entity.ObservationDataType;
import com.boot.study.entity.ObservationDataVO;
import com.boot.study.entity.VisualizationScene;
import com.boot.study.entity.VisualizationSceneVO;
import com.boot.study.enums.PublicFlagEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.response.PageBean;
import com.boot.study.service.ObservationDataService;
import com.boot.study.service.ObservationDataTypeService;
import com.boot.study.service.VisualizationSceneService;
import com.boot.study.utils.DateTimeUtil;
import com.boot.study.utils.EnumConvertUtil;
import com.boot.study.utils.PageHelperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 可视化场景表 Service 实现
 *
 * @author study
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisualizationSceneServiceImpl extends BaseServiceImpl<VisualizationSceneMapper, VisualizationScene> implements VisualizationSceneService {

    private final ObservationDataService observationDataService;
    private final ObservationDataTypeService observationDataTypeService;

    @Override
    public PageBean<VisualizationScene> pageList(VisualizationScenePageReq req) {
        LambdaQueryWrapper<VisualizationScene> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(req.getSceneName())) {
            wrapper.like(VisualizationScene::getSceneName, req.getSceneName());
        }
        if (req.getSceneType() != null) {
            wrapper.eq(VisualizationScene::getSceneType, req.getSceneType());
        }
        if (req.getIsPublic() != null) {
            wrapper.eq(VisualizationScene::getIsPublic, req.getIsPublic());
        }
        wrapper.orderByDesc(VisualizationScene::getCreatedTime);

        Page<VisualizationScene> page = this.page(PageHelperUtil.page(req), wrapper);
        return PageBean.page(page, page.getRecords());
    }

    @Override
    public PageBean<VisualizationSceneVO> pageListWithVO(VisualizationScenePageReq req) {
        PageBean<VisualizationScene> pageBean = pageList(req);
        List<VisualizationSceneVO> voList = pageBean.getList().stream()
                .map(EnumConvertUtil::convertVisualizationScene)
                .collect(Collectors.toList());

        PageBean<VisualizationSceneVO> voPageBean = new PageBean<>();
        voPageBean.setPageNum(pageBean.getPageNum());
        voPageBean.setPageSize(pageBean.getPageSize());
        voPageBean.setSize(pageBean.getSize());
        voPageBean.setTotal(pageBean.getTotal());
        voPageBean.setPages(pageBean.getPages());
        voPageBean.setList(voList);
        return voPageBean;
    }

    /**
     * 用户端 - 分页查询公开的场景配置
     * <p>
     * 只查询公开且未删除的场景配置，支持按场景名称、场景类型筛选
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果（VO对象，包含枚举描述字段）
     */
    @Override
    public PageBean<VisualizationSceneVO> getPublicScenes(UserScenePageReq req) {
        LambdaQueryWrapper<VisualizationScene> wrapper = Wrappers.lambdaQuery();
        // 只查询公开场景
        wrapper.eq(VisualizationScene::getIsPublic, PublicFlagEnum.YES);
        wrapper.eq(VisualizationScene::getDeletedFlag, false); // 未删除

        // 场景名称模糊查询
        if (StringUtils.hasText(req.getSceneName())) {
            wrapper.like(VisualizationScene::getSceneName, req.getSceneName().trim());
        }
        // 场景类型精确查询
        if (req.getSceneType() != null) {
            wrapper.eq(VisualizationScene::getSceneType, req.getSceneType());
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(VisualizationScene::getCreatedTime);

        // 分页查询
        Page<VisualizationScene> page = this.page(PageHelperUtil.page(req), wrapper);

        // 转换为VO
        List<VisualizationSceneVO> voList = page.getRecords().stream()
                .map(EnumConvertUtil::convertVisualizationScene)
                .collect(Collectors.toList());

        // 构建VO分页结果
        PageBean<VisualizationSceneVO> pageBean = new PageBean<>();
        pageBean.setList(voList);
        pageBean.setTotal((int) page.getTotal());
        pageBean.setPageNum(req.getPageNum());
        pageBean.setPageSize(req.getPageSize());
        pageBean.setSize(page.getRecords().size());
        pageBean.setPages((int) page.getPages());

        return pageBean;
    }

    /**
     * 用户端 - 根据ID获取公开的场景详情
     * <p>
     * 验证场景是否存在、是否公开、是否已删除，并增加查看次数
     *
     * @param id 场景ID
     * @return 场景配置VO对象
     * @throws ServiceException 如果场景不存在或无权访问
     */
    @Override
    public VisualizationSceneVO getPublicSceneById(Long id) {
        VisualizationScene scene = this.getById(id);
        if (scene == null || Boolean.TRUE.equals(scene.getDeletedFlag())) {
            throw new ServiceException(404, "场景不存在");
        }
        if (scene.getIsPublic() != PublicFlagEnum.YES) {
            throw new ServiceException(403, "无权访问该场景");
        }

        // 增加查看次数
        if (scene.getViewCount() == null) {
            scene.setViewCount(0);
        }
        scene.setViewCount(scene.getViewCount() + 1);
        this.updateById(scene);

        return EnumConvertUtil.convertVisualizationScene(scene);
    }

    /**
     * 用户端 - 查询场景关联的观测数据
     * <p>
     * 验证场景是否存在且公开，然后查询关联的观测数据，支持经纬度范围过滤
     *
     * @param sceneId 场景ID
     * @param req 数据查询请求
     * @return 观测数据分页结果
     * @throws ServiceException 如果场景不存在或无权访问
     */
    @Override
    public PageBean<ObservationDataVO> getSceneData(Long sceneId, SceneDataQueryReq req) {
        // 验证场景是否存在且公开
        VisualizationScene scene = this.getById(sceneId);
        if (scene == null || Boolean.TRUE.equals(scene.getDeletedFlag())) {
            throw new ServiceException(404, "场景不存在");
        }
        if (scene.getIsPublic() != PublicFlagEnum.YES) {
            throw new ServiceException(403, "无权访问该场景");
        }

        // 设置场景ID
        req.setSceneId(sceneId);

        // 构建查询条件
        ObservationDataPageReq dataReq = new ObservationDataPageReq();
        dataReq.setPageNum(req.getPageNum());
        dataReq.setPageSize(req.getPageSize());

        // 1. 先从场景配置 configJson 中解析出需要的数据类型（dataBindings 里的 *TypeCode）
        if (StringUtils.hasText(scene.getConfigJson())) {
            try {
                com.alibaba.fastjson2.JSONObject config =
                        com.alibaba.fastjson2.JSON.parseObject(scene.getConfigJson());
                if (config != null) {
                    List<String> typeCodes = new ArrayList<>();
                    // 顶层 dataQuery 中可能配置了 dataSourceId
                    com.alibaba.fastjson2.JSONObject dataQuery = config.getJSONObject("dataQuery");
                    if (dataQuery != null) {
                        Long cfgDataSourceId = dataQuery.getLong("dataSourceId");
                        if (cfgDataSourceId != null) {
                            dataReq.setDataSourceId(cfgDataSourceId);
                        }
                    }

                    // layers[*].dataBindings 下的 *TypeCode，全部收集
                    List<com.alibaba.fastjson2.JSONObject> layers = config.getList("layers", com.alibaba.fastjson2.JSONObject.class);
                    if (layers != null) {
                        for (com.alibaba.fastjson2.JSONObject layer : layers) {
                            com.alibaba.fastjson2.JSONObject bindings = layer.getJSONObject("dataBindings");
                            if (bindings != null) {
                                for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                                    Object v = entry.getValue();
                                    if (v instanceof String && ((String) v).length() > 0) {
                                        typeCodes.add((String) v);
                                    }
                                }
                            }
                        }
                    }

                    if (!typeCodes.isEmpty()) {
                        List<ObservationDataType> types = observationDataTypeService.list(
                                Wrappers.<ObservationDataType>lambdaQuery()
                                        .in(ObservationDataType::getTypeCode, typeCodes)
                        );
                        if (!types.isEmpty()) {
                            List<Long> typeIds = types.stream()
                                    .map(ObservationDataType::getId)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
                            dataReq.setDataTypeIds(typeIds);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("解析场景 configJson 失败, sceneId={}", sceneId, e);
            }
        }

        // 2. 再叠加前端传入的过滤条件（如 dataSourceId / dataTypeId / 时间范围等）
        if (req.getDataSourceId() != null) {
            dataReq.setDataSourceId(req.getDataSourceId());
        }
        if (req.getDataTypeId() != null) {
            dataReq.setDataTypeId(req.getDataTypeId());
            dataReq.setDataTypeIds(null);
        }
        if (req.getTypeCodes() != null && !req.getTypeCodes().isEmpty()) {
            // 若显式指定 typeCodes，则以其为准（覆盖场景 configJson 解析出来的类型集合）
            List<ObservationDataType> types = observationDataTypeService.list(
                    Wrappers.<ObservationDataType>lambdaQuery()
                            .in(ObservationDataType::getTypeCode, req.getTypeCodes())
            );
            if (!types.isEmpty()) {
                List<Long> typeIds = types.stream()
                        .map(ObservationDataType::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                dataReq.setDataTypeId(null);
                dataReq.setDataTypeIds(typeIds);
            }
        }
        dataReq.setStartTime(DateTimeUtil.parseToLocalDateTime(req.getStartTime()));
        dataReq.setEndTime(DateTimeUtil.parseToLocalDateTime(req.getEndTime()));
        // 经纬度范围过滤下推到数据库（避免分页后再过滤导致结果/total 不准确）
        dataReq.setMinLongitude(req.getMinLongitude());
        dataReq.setMaxLongitude(req.getMaxLongitude());
        dataReq.setMinLatitude(req.getMinLatitude());
        dataReq.setMaxLatitude(req.getMaxLatitude());

        // 查询数据
        PageBean<ObservationDataVO> pageBean = observationDataService.pageListWithRelation(dataReq);

        return pageBean;
    }
}


