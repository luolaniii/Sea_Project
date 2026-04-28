package com.boot.study.controller.user;

import com.boot.study.bean.LoginInfo;
import com.boot.study.bean.Result;
import com.boot.study.entity.Badge;
import com.boot.study.entity.UserBadge;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 - 徽章
 */
@RestController
@RequestMapping("/api/user/badge")
@RequiredArgsConstructor
public class UserBadgeController {

    private final BadgeService badgeService;

    /** 全部启用徽章（含锁定状态） */
    @GetMapping("/catalog")
    public Result<List<Badge>> catalog() {
        return Result.success(badgeService.listActive());
    }

    /** 我已获得的徽章 */
    @GetMapping("/me")
    public Result<List<UserBadge>> mine() {
        Long userId = LoginInfo.getUserId();
        if (userId == null) throw new ServiceException(ResultEnum.NOT_TOKEN);
        return Result.success(badgeService.listUserBadges(userId));
    }
}
