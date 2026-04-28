package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.study.dao.ExpertApplicationMapper;
import com.boot.study.entity.ExpertApplication;
import com.boot.study.entity.SysUser;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.ExpertApplicationService;
import com.boot.study.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 专家申请 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertApplicationServiceImpl extends BaseServiceImpl<ExpertApplicationMapper, ExpertApplication>
        implements ExpertApplicationService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private final SysUserService sysUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpertApplication submit(ExpertApplication form) {
        if (form == null || form.getUserId() == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "用户ID不能为空");
        }
        ExpertApplication pending = getOne(new LambdaQueryWrapper<ExpertApplication>()
                .eq(ExpertApplication::getUserId, form.getUserId())
                .eq(ExpertApplication::getStatus, STATUS_PENDING)
                .last("limit 1"));
        if (pending != null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(),
                    "您已有一个待审核的申请，请耐心等待管理员审核");
        }
        // 已是专家则禁止再次申请
        SysUser user = sysUserService.getById(form.getUserId());
        if (user != null && "expert".equalsIgnoreCase(user.getRole())) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "您已是专家用户");
        }
        form.setStatus(STATUS_PENDING);
        form.setReviewerId(null);
        form.setReviewRemark(null);
        form.setReviewedAt(null);
        save(form);
        return form;
    }

    @Override
    public ExpertApplication getMyLatest(Long userId) {
        if (userId == null) return null;
        return getOne(new LambdaQueryWrapper<ExpertApplication>()
                .eq(ExpertApplication::getUserId, userId)
                .orderByDesc(ExpertApplication::getCreatedTime)
                .last("limit 1"));
    }

    @Override
    public IPage<ExpertApplication> adminPage(Integer pageNum, Integer pageSize, String status, String keyword) {
        int p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int s = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        LambdaQueryWrapper<ExpertApplication> q = new LambdaQueryWrapper<ExpertApplication>()
                .orderByAsc(ExpertApplication::getStatus)
                .orderByDesc(ExpertApplication::getCreatedTime);
        if (status != null && !status.isEmpty()) {
            q.eq(ExpertApplication::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            q.and(w -> w.like(ExpertApplication::getRealName, kw)
                    .or().like(ExpertApplication::getOrganization, kw)
                    .or().like(ExpertApplication::getProfession, kw)
                    .or().like(ExpertApplication::getExpertiseTags, kw));
        }
        return page(new Page<>(p, s), q);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpertApplication review(Long id, boolean approve, String remark, Long reviewerId) {
        ExpertApplication app = getById(id);
        if (app == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "申请不存在");
        }
        if (!Objects.equals(app.getStatus(), STATUS_PENDING)) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "申请已被审核，无法重复操作");
        }
        app.setStatus(approve ? STATUS_APPROVED : STATUS_REJECTED);
        app.setReviewerId(reviewerId);
        app.setReviewRemark(remark);
        app.setReviewedAt(LocalDateTime.now());
        updateById(app);

        if (approve) {
            SysUser user = sysUserService.getById(app.getUserId());
            if (user != null && !"expert".equalsIgnoreCase(user.getRole())) {
                SysUser update = new SysUser();
                update.setId(user.getId());
                update.setRole("expert");
                if (app.getProfession() != null && !app.getProfession().isEmpty()
                        && (user.getExpertTitle() == null || user.getExpertTitle().isEmpty())) {
                    update.setExpertTitle(app.getProfession());
                }
                if (app.getExpertiseTags() != null && !app.getExpertiseTags().isEmpty()
                        && (user.getExpertField() == null || user.getExpertField().isEmpty())) {
                    update.setExpertField(app.getExpertiseTags());
                }
                sysUserService.updateById(update);
                log.info("用户 {} 已升级为专家（applicationId={}）", user.getId(), id);
            }
        }
        return app;
    }
}
