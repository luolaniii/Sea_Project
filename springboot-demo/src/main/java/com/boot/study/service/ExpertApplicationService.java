package com.boot.study.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.study.entity.ExpertApplication;

/**
 * 专家申请 Service
 */
public interface ExpertApplicationService extends BaseService<ExpertApplication> {

    /** 提交申请（同一用户存在 PENDING 时禁止重复） */
    ExpertApplication submit(ExpertApplication form);

    /** 当前用户最近一次申请（无则返回 null） */
    ExpertApplication getMyLatest(Long userId);

    /** 管理员分页查询 */
    IPage<ExpertApplication> adminPage(Integer pageNum, Integer pageSize, String status, String keyword);

    /**
     * 审核
     *
     * @param id        申请ID
     * @param approve   是否通过
     * @param remark    审核备注
     * @param reviewerId 审核人ID
     */
    ExpertApplication review(Long id, boolean approve, String remark, Long reviewerId);
}
