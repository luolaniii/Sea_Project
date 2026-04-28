package com.boot.study.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.study.bean.LoginInfo;
import com.boot.study.bean.Result;
import com.boot.study.entity.ExpertApplication;
import com.boot.study.enums.ResultEnum;
import com.boot.study.service.ExpertApplicationService;
import com.boot.study.utils.PathLongParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端 - 专家申请审核
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/expert-application")
@RequiredArgsConstructor
public class ExpertApplicationAdminController {

    private final ExpertApplicationService expertApplicationService;

    /** 列表分页 */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword) {
        IPage<ExpertApplication> p = expertApplicationService.adminPage(pageNum, pageSize, status, keyword);
        Map<String, Object> data = new HashMap<>();
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("pageNum", p.getCurrent());
        data.put("pageSize", p.getSize());
        return Result.success(data);
    }

    /** 审核 */
    @PostMapping("/{id}/review")
    public Result<ExpertApplication> review(@PathVariable("id") String idStr,
                                            @RequestBody Map<String, Object> body) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        if (body == null) return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "缺少审核参数");
        Boolean approve = body.get("approve") instanceof Boolean ? (Boolean) body.get("approve") : null;
        if (approve == null) {
            Object o = body.get("approve");
            if (o != null) approve = Boolean.parseBoolean(String.valueOf(o));
        }
        if (approve == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "approve 不能为空");
        }
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        return Result.success(expertApplicationService.review(id, approve, remark, LoginInfo.getUserId()));
    }
}
