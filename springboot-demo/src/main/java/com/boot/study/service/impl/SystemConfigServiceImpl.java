package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.study.api.req.admin.SystemConfigPageReq;
import com.boot.study.dao.SystemConfigMapper;
import com.boot.study.entity.SystemConfig;
import com.boot.study.entity.SystemConfigVO;
import com.boot.study.response.PageBean;
import com.boot.study.service.SystemConfigService;
import com.boot.study.utils.EnumConvertUtil;
import com.boot.study.utils.PageHelperUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统配置表 Service 实现
 *
 * @author study
 */
@Slf4j
@Service
public class SystemConfigServiceImpl extends BaseServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    @Override
    public PageBean<SystemConfig> pageList(SystemConfigPageReq req) {
        LambdaQueryWrapper<SystemConfig> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(req.getConfigKey())) {
            wrapper.like(SystemConfig::getConfigKey, req.getConfigKey());
        }
        if (StringUtils.hasText(req.getConfigGroup())) {
            wrapper.eq(SystemConfig::getConfigGroup, req.getConfigGroup());
        }
        if (req.getConfigType() != null) {
            wrapper.eq(SystemConfig::getConfigType, req.getConfigType());
        }
        if (req.getIsSystem() != null) {
            wrapper.eq(SystemConfig::getIsSystem, req.getIsSystem());
        }
        wrapper.orderByDesc(SystemConfig::getCreatedTime);

        Page<SystemConfig> page = this.page(PageHelperUtil.page(req), wrapper);
        return PageBean.page(page, page.getRecords());
    }

    @Override
    public PageBean<SystemConfigVO> pageListWithVO(SystemConfigPageReq req) {
        PageBean<SystemConfig> pageBean = pageList(req);
        List<SystemConfigVO> voList = pageBean.getList().stream()
                .map(EnumConvertUtil::convertSystemConfig)
                .collect(Collectors.toList());

        PageBean<SystemConfigVO> voPageBean = new PageBean<>();
        voPageBean.setPageNum(pageBean.getPageNum());
        voPageBean.setPageSize(pageBean.getPageSize());
        voPageBean.setSize(pageBean.getSize());
        voPageBean.setTotal(pageBean.getTotal());
        voPageBean.setPages(pageBean.getPages());
        voPageBean.setList(voList);
        return voPageBean;
    }
}


