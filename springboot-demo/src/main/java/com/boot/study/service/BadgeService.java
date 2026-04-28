package com.boot.study.service;

import com.boot.study.entity.Badge;
import com.boot.study.entity.UserBadge;

import java.util.List;

/**
 * 徽章 Service
 */
public interface BadgeService extends BaseService<Badge> {

    /** 启用的徽章按 sortOrder 排序 */
    List<Badge> listActive();

    /** 用户已获得的徽章列表（按解锁时间倒序） */
    List<UserBadge> listUserBadges(Long userId);
}
