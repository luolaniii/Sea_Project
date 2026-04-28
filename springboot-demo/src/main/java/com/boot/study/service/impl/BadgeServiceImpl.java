package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boot.study.dao.BadgeMapper;
import com.boot.study.dao.UserBadgeMapper;
import com.boot.study.entity.Badge;
import com.boot.study.entity.UserBadge;
import com.boot.study.service.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 徽章 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeServiceImpl extends BaseServiceImpl<BadgeMapper, Badge> implements BadgeService {

    private final UserBadgeMapper userBadgeMapper;

    @Override
    public List<Badge> listActive() {
        return list(new LambdaQueryWrapper<Badge>()
                .eq(Badge::getStatus, 1)
                .orderByAsc(Badge::getSortOrder)
                .orderByAsc(Badge::getThresholdCount));
    }

    @Override
    public List<UserBadge> listUserBadges(Long userId) {
        return userBadgeMapper.selectList(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .orderByDesc(UserBadge::getAwardedAt));
    }
}
