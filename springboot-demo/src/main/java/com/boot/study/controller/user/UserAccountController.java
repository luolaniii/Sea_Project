package com.boot.study.controller.user;

import com.boot.study.bean.LoginInfo;
import com.boot.study.bean.Result;
import com.boot.study.bean.UserInfo;
import com.boot.study.consts.BaseConst;
import com.boot.study.entity.SysUser;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.SysUserService;
import com.boot.study.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 用户端 - 账户相关接口
 * <p>
 * 提供头像上传、个人资料获取等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/user/account")
@RequiredArgsConstructor
public class UserAccountController {

    private final SysUserService sysUserService;

    /** 头像最大 5MB */
    private static final long MAX_AVATAR_SIZE = 5L * 1024L * 1024L;

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "png", "jpg", "jpeg", "gif", "webp"
    ));

    /**
     * 获取当前登录用户最新信息（含头像）
     */
    @GetMapping("/me")
    public Result<UserInfo> me() {
        Long userId = LoginInfo.getUserId();
        if (userId == null) {
            return Result.fail(ResultEnum.NOT_TOKEN);
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "用户不存在");
        }
        UserInfo current = LoginInfo.getLoginInfo();
        UserInfo info = UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .avatar(user.getAvatar())
                // token 由前端持有；这里回填一个标记位避免前端误清空
                .token(current != null ? current.getToken() : null)
                .build();
        return Result.success(info);
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public Result<Map<String, Object>> uploadAvatar(@RequestPart("file") MultipartFile file) {
        Long userId = LoginInfo.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultEnum.NOT_TOKEN);
        }
        if (file == null || file.isEmpty()) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "上传文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "头像大小不能超过 5MB");
        }
        String originalName = file.getOriginalFilename() == null ? "avatar.png" : file.getOriginalFilename();
        String safeName = Paths.get(originalName).getFileName().toString();
        String ext = "";
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < safeName.length() - 1) {
            ext = safeName.substring(dotIndex);
        }
        String extWithoutDot = ext.startsWith(".") ? ext.substring(1).toLowerCase() : ext.toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extWithoutDot)) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(),
                    "仅支持 png/jpg/jpeg/gif/webp 格式");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extWithoutDot;

        Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "avatar");
        Path target = dir.resolve(storedName);
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("头像上传失败", e);
            return Result.fail(500, "上传失败: " + e.getMessage());
        }

        String avatarUrl = "/uploads/avatar/" + storedName;

        // 更新 sys_user 的 avatar 字段
        SysUser update = new SysUser();
        update.setId(userId);
        update.setAvatar(avatarUrl);
        sysUserService.updateById(update);

        // 同步更新 Redis 缓存中的 UserInfo，避免下次请求拿到旧头像
        UserInfo current = LoginInfo.getLoginInfo();
        if (current != null && current.getRole() != null) {
            current.setAvatar(avatarUrl);
            String tokenPath = current.getRole() + ":" + current.getId();
            RedisUtil.set(BaseConst.TOKEN + tokenPath, current, BaseConst.EXPIRATION_TIME);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("avatar", avatarUrl);
        log.info("用户 {} 头像更新: {}", userId, avatarUrl);
        return Result.success(data);
    }
}
