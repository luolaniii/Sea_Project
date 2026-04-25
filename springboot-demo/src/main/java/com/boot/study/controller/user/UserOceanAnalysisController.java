package com.boot.study.controller.user;

import com.boot.study.api.req.user.OceanAnalysisReq;
import com.boot.study.api.req.user.OceanComparisonReq;
import com.boot.study.bean.Result;
import com.boot.study.entity.OceanAbnormalResultVO;
import com.boot.study.entity.OceanComfortResultVO;
import com.boot.study.entity.OceanComparisonResultVO;
import com.boot.study.entity.OceanCompositeResultVO;
import com.boot.study.entity.OceanStabilityResultVO;
import com.boot.study.entity.OceanTrendResultVO;
import com.boot.study.service.OceanAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/ocean-analysis")
@RequiredArgsConstructor
public class UserOceanAnalysisController {

    private final OceanAnalysisService oceanAnalysisService;

    @PostMapping("/abnormal")
    public Result<OceanAbnormalResultVO> abnormal(@RequestBody OceanAnalysisReq req) {
        return Result.success(oceanAnalysisService.abnormal(req));
    }

    @PostMapping("/stability")
    public Result<OceanStabilityResultVO> stability(@RequestBody OceanAnalysisReq req) {
        return Result.success(oceanAnalysisService.stability(req));
    }

    @PostMapping("/comfort")
    public Result<OceanComfortResultVO> comfort(@RequestBody OceanAnalysisReq req) {
        return Result.success(oceanAnalysisService.comfort(req));
    }

    @PostMapping("/trend")
    public Result<OceanTrendResultVO> trend(@RequestBody OceanAnalysisReq req) {
        return Result.success(oceanAnalysisService.trend(req));
    }

    @PostMapping("/composite")
    public Result<OceanCompositeResultVO> composite(@RequestBody OceanAnalysisReq req) {
        return Result.success(oceanAnalysisService.composite(req));
    }

    /**
     * Module I：多站点对比分析与空间异常检测
     */
    @PostMapping("/compare")
    public Result<OceanComparisonResultVO> compare(@RequestBody OceanComparisonReq req) {
        return Result.success(oceanAnalysisService.compare(req));
    }
}

