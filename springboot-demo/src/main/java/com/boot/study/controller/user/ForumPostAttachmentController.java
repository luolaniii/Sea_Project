package com.boot.study.controller.user;

import com.boot.study.bean.Result;
import com.boot.study.entity.ForumPostAttachment;
import com.boot.study.enums.ResultEnum;
import com.boot.study.service.ForumPostAttachmentService;
import com.boot.study.utils.PathLongParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 用户端 - 论坛帖子附件接口
 */
@Slf4j
@RestController
@RequestMapping("/api/user/forum-post-attachment")
@RequiredArgsConstructor
public class ForumPostAttachmentController {

    private final ForumPostAttachmentService forumPostAttachmentService;
    private static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "csv", "json", "txt", "pdf", "png", "jpg", "jpeg", "gif", "xlsx", "xls"
    ));

    @PostMapping
    public Result<ForumPostAttachment> addAttachment(@RequestBody ForumPostAttachment attachment) {
        return Result.success(forumPostAttachmentService.addAttachment(attachment));
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadFile(@RequestPart("file") MultipartFile file) {
        log.info("论坛附件上传请求，文件名: {}", file != null ? file.getOriginalFilename() : null);
        if (file == null || file.isEmpty()) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "文件大小不能超过20MB");
        }
        String originalName = file.getOriginalFilename() == null ? "unknown.dat" : file.getOriginalFilename();
        String safeName = Paths.get(originalName).getFileName().toString();
        String ext = "";
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < safeName.length() - 1) {
            ext = safeName.substring(dotIndex);
        }
        String extWithoutDot = ext.startsWith(".") ? ext.substring(1).toLowerCase() : ext.toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extWithoutDot)) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "不支持的文件类型，仅支持: csv/json/txt/pdf/png/jpg/jpeg/gif/xlsx/xls");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;

        Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "forum");
        Path target = dir.resolve(storedName);
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            Map<String, Object> data = new HashMap<>();
            data.put("fileName", safeName);
            data.put("fileUrl", "/uploads/forum/" + storedName);
            data.put("fileSize", file.getSize());
            log.info("论坛附件上传成功，保存路径: {}", target);
            return Result.success(data);
        } catch (IOException e) {
            log.error("论坛附件上传失败", e);
            return Result.fail(500, "上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/post/{postId}")
    public Result<List<ForumPostAttachment>> listByPostId(@PathVariable("postId") String postIdStr) {
        Long postId = PathLongParser.tryParse(postIdStr);
        if (postId == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "帖子ID无效");
        }
        return Result.success(forumPostAttachmentService.listByPostId(postId));
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteAttachment(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        log.info("论坛附件删除请求，id: {}", id);
        forumPostAttachmentService.deleteAttachment(id);
        log.info("论坛附件删除成功，id: {}", id);
        return Result.success();
    }
}
