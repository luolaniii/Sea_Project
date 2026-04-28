package com.boot.study.controller.user;

import com.boot.study.bean.LoginInfo;
import com.boot.study.bean.Result;
import com.boot.study.entity.ExpertApplication;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.ExpertApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 - 专家申请
 */
@Slf4j
@RestController
@RequestMapping("/api/user/expert-application")
@RequiredArgsConstructor
public class UserExpertApplicationController {

    private final ExpertApplicationService expertApplicationService;

    /** 提交申请 */
    @PostMapping
    public Result<ExpertApplication> submit(@RequestBody ExpertApplication form) {
        Long userId = LoginInfo.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultEnum.NOT_TOKEN);
        }
        form.setUserId(userId);
        return Result.success(expertApplicationService.submit(form));
    }

    /** 我最近一次申请 */
    @GetMapping("/me")
    public Result<ExpertApplication> myLatest() {
        Long userId = LoginInfo.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultEnum.NOT_TOKEN);
        }
        return Result.success(expertApplicationService.getMyLatest(userId));
    }
}
