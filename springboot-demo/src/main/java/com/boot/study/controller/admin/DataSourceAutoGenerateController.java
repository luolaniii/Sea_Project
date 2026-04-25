package com.boot.study.controller.admin;

import com.boot.study.bean.Result;
import com.boot.study.service.DataSourceAutoGenerateService;
import com.boot.study.utils.PathLongParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 数据源一键自动生成图表/场景（Module C）
 * <p>
 * 控制器层仅做参数校验 + 鉴权 + 结果包装，核心逻辑委托给 {@link DataSourceAutoGenerateService}。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/data-source")
@RequiredArgsConstructor
public class DataSourceAutoGenerateController {

    private final DataSourceAutoGenerateService autoGenerateService;

    @PostMapping("/{id}/auto-generate")
    public Result<Map<String, Object>> autoGenerate(@PathVariable("id") String idStr) {
        Long dsId = PathLongParser.tryParse(idStr);
        if (dsId == null) {
            return Result.fail(400, "数据源ID无效");
        }
        Map<String, Object> result = autoGenerateService.generate(dsId);
        if (result.get("error") != null) {
            return Result.fail(400, String.valueOf(result.get("error")));
        }
        log.info("自动生成完成 dsId={}, charts={}, scenes={}",
                dsId, result.get("createdCharts"), result.get("createdScenes"));
        return Result.success(result);
    }
}
