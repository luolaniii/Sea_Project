package com.boot.study.api.req.user;

import com.boot.study.api.req.base.QueryReq;
import com.boot.study.enums.SceneTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户端 - 场景分页查询请求（只查询公开场景）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserScenePageReq extends QueryReq {

    /**
     * 场景名称，模糊匹配
     */
    private String sceneName;

    /**
     * 场景类型
     */
    private SceneTypeEnum sceneType;
}

